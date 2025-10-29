import { useEffect, useRef, useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group, Text, Title, Loader, Center, Progress } from '@mantine/core';

import { useForm } from '@mantine/form';

import '@mantine/core/styles.css';
import { parseFilenameFromContentDisposition } from './utils';

import { color } from './themes';

import SockJs from "sockjs-client";
import { Client } from '@stomp/stompjs';
import { v4 as uuidv4 } from "uuid";
import axios from "axios";
import eruda from "eruda";

const address: string = window.location.protocol + "//" + window.location.hostname;
let api: string = address;

if (window.location.hostname.indexOf("localhost") > -1 || window.location.hostname.indexOf("192.168.") > -1) {
  api += ":3000";
}
api += "/api/v1";

eruda.init();

interface DownloadRequest {
  requestType: string | undefined,
  url: string,
  videoQuality?: number,
  videoFormat?: string,
  audioFormat?: string,
  outputName?: string
};

// interface DownloadResponse {
//   requestId: string
// };

interface StatusResponse {
  status: string,
  message: string | null
};

interface ApiError {
  code: string,
  message: string
}

function App() {

  const [apiError, setApiError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);
  // const [isPolling, setIsPolling] = useState(false);
  const [downloadStatus, setDownloadStatus] = useState<string | null>(null);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isDownloaded, setIsDownloaded] = useState(false);
  const [isCancelled, setIsCancelled] = useState(false);
  const [ext, setExt] = useState("");

  // console.log("RequestID:", requestId);

  const mediaTypes: string[] = ["Video", "Video Only", "Audio Only"];
  const videoQualities: string[] = ["144p", "240p", "360p", "480p", "720p", "1080p", "2160p"];
  const videoFormats: string[] = ["mp4", "mkv"];
  const audioFormats: string[] = ["mp3", "m4a", "wav", "flac"];
  // const pollInterval: number = 2000;

  const mediaTypeMap = new Map<string, string>([
    ["Video", "video"],
    ["Video Only", "video_only"],
    ["Audio Only", "audio_only"]
  ]);

  const form = useForm({
    mode: 'uncontrolled',
    initialValues: {
      type: "Video",
      url: "",
      videoQuality: "720p",
      videoFormat: "Default",
      audioFormat: "Default",
      outputName: ""
    },
    validate: {
      url: (value) => {
        try {
          new URL(value);
          return null;
        } catch (error) {
          return "Invalid URL";
        }
      }
    }
  });

  type FormValues = typeof form.values;
  const type = form.getValues().type;
  const isVideo: boolean = (type === "Video" || type === "Video Only");

  const currentAbort = useRef<AbortController | null>(null);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      // abort any pending request
      currentAbort.current?.abort();
    };
  }, []);
  
  const [chunkSizeBytes, setChunkSizeBytes] = useState(1024);
  let [percentage, setPercentage] = useState(0.0);
  let [chunkFile, setChunkFile] = useState(new Uint8Array());
  let [pBytes, setPBytes] = useState(0.0);
  let [socket, setSocket] = useState(new SockJs(api + "/ws"));
  let [stompClient, setStompClient] = useState(null);
  let [myId, setMyId] = useState(uuidv4());
  let [videoId, setVideoId] = useState(uuidv4());
  let [videoName, setVideoName] = useState("");

  useEffect(function() {
    //stompClient.activate();
    var a = 0;
    var imgLink = "https://www.google.com/images/phd/px.gif";
    var img = new Image();
    var chunkSizeBytesTemp = 0;
    function testLatency(num) {
      if (num < 10) {
        var tStart = new Date().getTime();
        img.src = imgLink + "?t=" + tStart;
        img.onload = function () {
          var tEnd = new Date().getTime();
          var tTimeTook = tEnd - tStart;
          a += tTimeTook;
          testLatency(num + 1);
        }
        //alert();
      } else {
        var avg = a / 10;
        //alert(avg);
        chunkSizeBytesTemp = Math.ceil(16384 / (1 + (avg / 200) ** 2));
        setChunkSizeBytes(chunkSizeBytesTemp);
        //alert(chunkSizeBytesTemp);
      }
    }
    testLatency(0);
  }, []);

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      console.log('Clipboard content:', text);
      form.setFieldValue("url", text);
    } catch (err: any) {
      console.error('Failed to read clipboard contents:', err);
      return null; 
    }
  }

  const transformRequest = (values: FormValues): DownloadRequest => {
    const request: DownloadRequest = {
      requestType: mediaTypeMap.get(values.type),
      url: values.url
    }

    if(values.type === "Audio Only") {
      if(values.audioFormat !== "Default") {
        request.audioFormat = values.audioFormat;
      }
    } else {
      request.videoQuality = parseInt(values.videoQuality);
      if(values.videoFormat !== "Default") {
        request.videoFormat = values.videoFormat;
      }
    }

    return request;
  }


  const reset = () => {
    setApiError(null);
    setRequestId(null);
    // setIsPolling(false);
    setDownloadStatus(null);
    setIsDownloaded(false);
    setIsSubmitted(false);
    setIsCancelled(false);
  }

  const handleSubmit = async (values: FormValues): Promise<any> => {
    if(isSubmitted) {
      return;
    }

    // let u: any = uuidv4();

    // setVideoId(u);

    setDownloadStatus(null);
    setIsDownloaded(false);
    setIsSubmitted(true);
    setApiError(null);
    setPercentage(0);
    setPBytes(0);

    let vidName: any = values.outputName;

    if (vidName == "") {
      vidName = videoId;
    }

    setVideoName(vidName);

    console.log("Form Values:", values);
    const request = transformRequest(values);
    console.log("Request Data:", request);

    request.id = myId;
    request.mediaId = videoId;
    console.log(socket);
    let sjs: any = new SockJs(api + "/ws");
    let sc: any = new Client({
      webSocketFactory: () => sjs,
      debug: (str) => console.log(str),
      onConnect: async () => {
        console.log("Connected!");

        let pp = 0;
        let u8array;
        let u8arraySize;
        let u8arrayCurrentSize;

        let sub = sc.subscribe("/topic/" + myId, async function (msg) {
          //console.log(msg.body);
          var l = msg.body.split(" ");
          if (l[0] == "Percent:") {
            //console.log(parseFloat(l[1]));
            var p = parseFloat(l[1]);
            if (pp < 10 && p > 50) {
              //alert();
              return;
            } else if(p > pp){
              //setPercentage(p);
              pp = p;
            }
            setPercentage(pp);
            //document.getElementById("percent").innerText = pp;
          } else if (l[0] == "Url:") {
            //alert(l[1])
            var str = l[1];
            var i = str.length - 1;
            var ext = "";
            while (i > -1 && str[i] != '.') {
              ext = str[i] + ext;
              i--;
            }
            //alert(ext);
            setExt(ext);
          }else if (l[0] == "Done") {
            //alert();
            pp = 0;
            //alert(videoId);
            //alert(chunkSizeBytes);
            //setDownloadStatus("success");
            //setIsSubmitted(false);
            sc.publish({
              destination: "/app/wschunk",
              body: JSON.stringify({
                id: myId,
                mediaId: videoId,
                chunkSize: chunkSizeBytes,
                message: "Download"
              })
            });
          } else if (l[0] == "Data") {
            var index = parseInt(l[1]);
            var bytesRead = parseInt(l[2]);
            var binSize = parseInt(l[3]);
            var payload = l[4];
            var bPayload = atob(payload);
            //console.log(bPayload.length);

            //alert(bytesRead);
            if (index == 0) {
              u8array = new Uint8Array(binSize);
              u8arrayCurrentSize = 0;
              u8arraySize = binSize;
            }
            var start = index * chunkSizeBytes;
            for (var i = 0; i < bytesRead; i++) {
              u8array[start + i] = bPayload.charCodeAt(i);
            }


            u8arrayCurrentSize += bytesRead;
            setPBytes(u8arrayCurrentSize / u8arraySize * 100);
            if (u8arrayCurrentSize == u8arraySize) {
              //alert("Done!!!")
              setChunkFile(u8array);
              u8array = new Uint8Array();
              setDownloadStatus("success");
              setIsSubmitted(false);
              setVideoId(uuidv4());
              sub.unsubscribe();
              sjs.close();
              await sc.deactivate();
              //alert("Deactivate");
            }
          }
        })

        //alert("sc wow")
        sc.publish({
          destination: "/app/wsdownload",
          body: JSON.stringify(request)
        });
      },
      onDisconnect: function() {
        //alert("Disconnect success!");
      }
    });

    /* if (stompClient && stompClient.active) {
      stompClient.deactivate();
      alert("Deactivate");
      setStompClient(sc)
    } */

    sc.activate();

    //alert("cb: " + chunkSizeBytes)
  }

  const cancelRequest = async () => {
    setIsCancelled(true);
    try {
      const response = await fetch(api + `/downloads/${requestId}`, {
        method: "DELETE",
        headers: { "Accept": "application/json" },
      });

      if(!response.ok) {
        const result: ApiError = await response.json();
        throw new Error(result.message);
      }

      const data: StatusResponse = await response.json();

      if(data.status === "success") {
        reset();
      }

    } catch(error: any) {
      console.error(error.message);
    }
  }

  const downloadFile = async () => {
    /* axios.get(api + "/d/" + videoId, {
      responseType: "blob"
    }).then(function(data) {
      const url = window.URL.createObjectURL(new Blob([data.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", videoName + "." + ext);
      document.body.appendChild(link);
      link.click();
      link.remove();
    }); */
    const url = window.URL.createObjectURL(new Blob([chunkFile]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", videoName + "." + ext);
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  return (
    <MantineProvider defaultColorScheme="light">
      <Flex pl="10%" pr="10%" h="100vh" direction="column" justify="center" align="center" gap="lg">
        <Title order={2}>Media Downloader</Title>
        <form style={{width: 420, maxWidth: "100%"}} onSubmit={form.onSubmit((values) => handleSubmit(values))}>
          <Flex w='100%' direction="column" rowGap="lg">
            <NativeSelect {...form.getInputProps('type')} label='Type' withAsterisk key={form.key("type")} data={mediaTypes} />
            <Group w="100%" gap="0" align='flex-end'>
              <TextInput {...form.getInputProps('url')}
                label='URL' withAsterisk key={form.key("url")} 
                placeholder='Enter video link here'
                w="100%"
                rightSection={
                  <Button type='button' bg={color.light[0]} radius={2} onClick={handlePaste} h='100%' w='100%' p={0} m={0}>Paste</Button>
                }
                rightSectionWidth={75}
              />
            </Group>
            {
              isVideo ? (
                <Group justify='space-between'>
                  <NativeSelect w='45%' {...form.getInputProps('videoQuality')} label='Video Quality' withAsterisk key={form.key("videoQuality")} data={videoQualities} />
                  <NativeSelect w='45%' {...form.getInputProps('videoFormat')} label='Video Format' withAsterisk key={form.key("videoFormat")} data={["Default", ...videoFormats]} />
                </Group>
              ) : (
                <NativeSelect {...form.getInputProps('audioFormat')} label='Audio Format' withAsterisk key={form.key("audioFormat")} data={["Default", ...audioFormats]} />
              )
            }
            <TextInput {...form.getInputProps('outputName')}
              label='Output Name'
              key={form.key("outputName")} 
              placeholder='Enter the name of the downloaded file'
            />
            {
              !isSubmitted && (
                <Button bg={color.light[0]} type='submit'>Fetch</Button>
              )
            }
            {
              (isSubmitted) && (
                <>
                  <Button type='button' bg={color.light[0]} disabled={isCancelled} onClick={cancelRequest}>Cancel</Button>
                  <Center>
                    <Loader color={color.light[0]} />
                    <Progress value={50} />
                  </Center>
                  <Progress value={percentage} />
                  <Progress value={pBytes} />
                </>
              )
            }
            {
              downloadStatus === "success" && (
                <Button type='button' disabled={isDownloaded} bg={color.light[0]} onClick={() => downloadFile()}>Download</Button>
              )
            }
            {
              apiError !== null && !isSubmitted && (
                <Center>
                  <Text c={color.light[0]}>{apiError}</Text>
                </Center>
              )
            }
          </Flex>
        </form>
      </Flex>
    </MantineProvider>
  );
}

export default App;
