import { useEffect, useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group, Text, Title, Grid, Center, Card } from '@mantine/core';

import { api, supportedSites } from './globals';
import { useForm } from '@mantine/form';

import '@mantine/core/styles.css';
import { parseFilenameFromContentDisposition } from './utils';

import { color } from './themes';

import useDownloadProgress from './hooks/useDownloadProgress';
import useWindowDimensions from './hooks/useWindowDimensions';

import ProgressBar from './components/ProgressBar';

interface DownloadRequest {
  requestType: string | undefined,
  url: string,
  videoQuality?: string,
  videoFormat?: string,
  audioQuality?: string,
  audioFormat?: string,
  embedMetadata: boolean,
  outputName?: string
};

interface StatusResponse {
  status: string,
  message: string | null
};

interface ApiError {
  code: string,
  message: string
}

// Fix problem with SSE connection causing error if download is already completed or ongoing when user pastes request ID
function App() {

  const { height, width } = useWindowDimensions();
  const isMobile: boolean = width < 700;

  const [apiError, setApiError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);
  const [downloadStatus, setDownloadStatus] = useState<string | null>(null);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isDownloaded, setIsDownloaded] = useState(false);
  const [isCancelled, setIsCancelled] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const { status, code, progress, message } = useDownloadProgress({requestId: requestId || "", url: requestId ? (api + `/downloads/${encodeURIComponent(requestId)}/status`) : null, status: downloadStatus || undefined});

  const mediaTypes: string[] = ["Video", "Video Only", "Audio Only"];
  const videoQualities: string[] = ["Best", "2160p", "1440p", "1080p", "720p", "480p", "360p", "240p", "144p", "Worst"];
  const videoFormats: string[] = ["mp4"];
  const audioQualities: string[] = ["Best", "320kbps", "256kbps", "192kbps", "128kbps", "Worst"];
  const audioFormats: string[] = ["Default"];

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
      videoQuality: "Best",
      videoFormat: "Default",
      audioQuality: "Best",
      audioFormat: "Default",
      embedMetadata: "Yes",
      outputName: "",
      requestId: requestId || ""
    },
    validate: {
      url: (value) => {
        if(isUlid(value)) {
          return null;
        }

        try {
          new URL(value);
          return null;
        } catch (error: any) {
          return "Invalid URL / Request ID";
        }
      }
    }
  });

  type FormValues = typeof form.values;
  const type = form.getValues().type;
  const isVideo: boolean = (type === "Video" || type === "Video Only");

  const [copyText, setCopyText] = useState("Copy");

  const capitalizeFirstLetter = (str: string): string => {
    if(!str) {
      return str;
    }

    const firstLetter = str.charAt(0).toUpperCase();
    const restOfString = str.slice(1);

    return firstLetter + restOfString;
  }

  const patchRequestForm = (values: DownloadRequest) => {
    let requestType: string = "Video";

    for(const [key, value] of mediaTypeMap.entries()) {
      if(value === values.requestType) {
        requestType = key;
        break;
      }
    }

    let videoQuality: string = values.videoQuality ? values.videoQuality : "Best";
    let videoFormat: string = values.videoFormat ? values.videoFormat : "Default";
    let audioQuality: string = values.audioQuality ? values.audioQuality : "Best";
    let audioFormat: string = values.audioFormat ? values.audioFormat : "Default";

    if(videoQuality === "best" || videoQuality === "worst") {
      videoQuality = capitalizeFirstLetter(videoQuality);
    }

    if(audioQuality === "best" || audioQuality === "worst") {
      audioQuality = capitalizeFirstLetter(audioQuality);
    }

    if(videoFormat === "default") {
      videoFormat = capitalizeFirstLetter(videoFormat);
    }

    if(audioFormat === "default") {
      audioFormat = capitalizeFirstLetter(audioFormat);
    }

    form.setFieldValue("type", requestType);
    form.setFieldValue("videoQuality", videoQuality);
    form.setFieldValue("videoFormat", videoFormat);
    form.setFieldValue("audioQuality", audioQuality);
    form.setFieldValue("audioFormat", audioFormat);
    form.setFieldValue("embedMetadata", values.embedMetadata ? "Yes" : "No");
  }

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      form.setFieldValue("url", text);
      
      if(isUlid(text)) {
        setRequestId(text);
        form.setFieldValue("requestId", text);
        const response = await fetch(api + `/downloads/${encodeURIComponent(text)}`, {
          method: "GET",
          headers: {
            "Content-Type": "application/json",
          }
        });

        if(!response.ok) {
          throw new Error(`Response status: ${response.status}`);
        }

        const data = await response.json();
        patchRequestForm(data);
      }
    } catch (err: any) {
      console.error('Failed to read clipboard contents:', err);
      return null; 
    }
  }

  const handleCopy = async () => {
    try {
      const requestId = form.getValues().requestId;
      if(!requestId) {
        return;
      }
      await navigator.clipboard.writeText(requestId);
      setCopyText("Copied!");
      setTimeout(() => setCopyText("Copy"), 1500);
    } catch (err: any) {
      console.error('Failed to copy to clipboard:', err);
      return null;
    }
  }

  const transformRequest = (values: FormValues): DownloadRequest => {
    const request: DownloadRequest = {
      requestType: mediaTypeMap.get(values.type),
      url: values.url,
      embedMetadata: (values.embedMetadata === "Yes") ? true : false,
    }

    if(request.requestType === "video" || request.requestType === "video_only") {
      request.videoQuality = values.videoQuality;
      request.videoFormat = values.videoFormat;
    } else if(request.requestType === "audio_only") {
      request.audioQuality = values.audioQuality;
    }

    return request;
  }

  const isUlid = (str: string): boolean => {
    const ulidRegex = /^[0-9A-HJKMNP-TV-Z]{26}$/;
    return ulidRegex.test(str);
  }

  const reset = () => {
    setApiError(null);
    setRequestId(null);
    setDownloadStatus(null);
    setIsDownloaded(false);
    setIsSubmitted(false);
    setIsCancelled(false);
    setDownloadProgress(0);
  }

  const handleSubmit = async (values: FormValues): Promise<any> => { // Start Download
    if(isSubmitted) {
      return;
    }

    setIsDownloaded(false);
    setIsSubmitted(true);
    setApiError(null);
    setDownloadStatus(null);
    setRequestId(null);

    const request = transformRequest(values);

    try {
      const response = await fetch(api + "/downloads", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(request)
      });

      if(!response.ok) {
        throw new Error(`Response status: ${response.status}`);
      }

      const data = await response.json();
      setRequestId(data.requestId);
      setDownloadStatus(data.status);
      form.setFieldValue("requestId", data.requestId);
      return data;
    } catch(error: any) {
      console.error(error);
      setApiError(error.message);
      return error;
    }
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
      console.log("Cancel Response:", data);
      if(data.status === "cancelled") {
        reset();
      }

    } catch(error: any) {
      console.error(error.message);
    }
  }

  const downloadFile = async () => {
    if(!requestId || isDownloaded) {
      return;
    }

    let url = api + `/downloads/${encodeURIComponent(requestId)}/file`;
    const outputName = form.getValues().outputName;

    if(outputName !== null && outputName.length > 0) {
      url += `?output=${encodeURIComponent(outputName)}`;
    }

    setIsDownloaded(true); // Prevent pressing of download button multiple times

    try {
      const response = await fetch(url, {
        method: "GET"
      });

      if(!response.ok) {
        const res: ApiError = await response.json();
        setApiError(res.message);
        return;
      }

      const contentDisposition = response.headers.get("Content-Disposition");
      const filename = parseFilenameFromContentDisposition(contentDisposition);

      if(!response.body) {
        const blobUrl = window.URL.createObjectURL(await response.blob());
        const a = document.createElement("a");
        a.href = blobUrl;

        if(!filename) {
          throw new Error("No file returned");
        }

        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => window.URL.revokeObjectURL(blobUrl), 10000);
        return;
      }

      const reader = response.body?.getReader();
      const contentLength = response.headers.get('Content-Length');
      let receivedLength = 0;
      const chunks = [];

      while(true) {
        const {done, value} = await reader.read();
        if(done) break;
        chunks.push(value);
        receivedLength += value.length;
        setDownloadProgress(prev => prev = Math.floor((receivedLength / (contentLength ? parseInt(contentLength) : 1)) * 100));
      }

      const blob = new Blob(chunks);
      const blobUrl = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = blobUrl;

      if(!filename) {
        throw new Error("No file returned");
      }

      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 10000);
      
    } catch(error: any) {
      setApiError(error.message);
    } finally {
      setIsDownloaded(false);
      setDownloadProgress(0);
    }
  };

  useEffect(() => {
    if(status === "pending") {
      setDownloadStatus(status);
      return;
    }

    if(status === "failed") {
      setApiError(message);
      setDownloadProgress(0);
      setDownloadStatus(status);
      setIsSubmitted(false);
      return;
    }

    if(status !== "completed") return;

    setDownloadStatus(status);
    setIsSubmitted(false);
  }, [status]);

  useEffect(() => {
    if(downloadStatus === "completed") {
      setDownloadProgress(0);
      setIsSubmitted(false);
    }
  }, [downloadStatus])

  useEffect(() => {
    setDownloadProgress(prev => prev = progress);
  }, [progress])

  let progressBarMessage = (downloadProgress > 0) ? `${message}: ${downloadProgress}%` : "Pending...";
  if(isDownloaded) { // 2nd phase of download (file download)
    progressBarMessage = (downloadProgress > 0) ? `Fetching: ${downloadProgress}%` : "Fetching resource...";
  }

  return (
    <MantineProvider defaultColorScheme="light">
      <Flex h="100vh" pb="30px" pl="10%" pr="10%" direction="column" justify="center" align="center">
        {/* <Title style={{color: "red"}} order={3}>Under Maintenance</Title> */}
        <Title order={2}>Media Downloader</Title>
        <form style={{width: 420, maxWidth: "100%"}} onSubmit={form.onSubmit((values) => handleSubmit(values))}>
          <Flex w='100%' direction="column" rowGap="lg">
            <NativeSelect {...form.getInputProps('type')} label="Type" withAsterisk key={form.key("type")} data={mediaTypes} onChange={(e) => form.setFieldValue("type", e.currentTarget.value)} />
            <Group w="100%" gap="0" align='flex-end'>
              <TextInput {...form.getInputProps('url')}
                label='URL / Request ID' withAsterisk key={form.key("url")} 
                placeholder='Enter a link or a previous request ID here'
                w="100%"
                rightSection={
                  <Button type='button' bg={color.light[0]} radius={2} onClick={handlePaste} h='100%' w='100%' p={0} m={0}>Paste</Button>
                }
                rightSectionWidth={75}
              />
            </Group>
            {
              isVideo ? (
                <Grid justify='space-between'>
                  <Grid.Col span={6}><NativeSelect w='100%' {...form.getInputProps('videoQuality')} label='Video Quality' withAsterisk key={form.key("videoQuality")} data={videoQualities} /></Grid.Col>
                  <Grid.Col span={6}><NativeSelect w='100%' {...form.getInputProps('videoFormat')} label='Video Format' withAsterisk key={form.key("videoFormat")} data={videoFormats} /></Grid.Col>
                </Grid>
              ) : (
                <Grid justify='space-between'>
                  <Grid.Col span={6}><NativeSelect w='100%' {...form.getInputProps('audioQuality')} label='Audio Quality' withAsterisk key={form.key("audioQuality")} data={audioQualities} /></Grid.Col>
                  <Grid.Col span={6}><NativeSelect w='100%' {...form.getInputProps('audioFormat')} label='Audio Format' withAsterisk key={form.key("audioFormat")} data={audioFormats} /></Grid.Col>
                </Grid>
              )
            }
            <Grid>
              <Grid.Col span={6}><NativeSelect w='100%' {...form.getInputProps('embedMetadata')} label='Embed Metadata' withAsterisk key={form.key("embedMetadata")} data={["Yes", "No"]} /></Grid.Col>
              <Grid.Col span={6}>
                <TextInput {...form.getInputProps('outputName')}
                  label='Output Name'
                  key={form.key("outputName")} 
                  placeholder='Enter name of file'
                />
              </Grid.Col>
            </Grid>
            <Group w="100%" gap="0" align='flex-end'>
              <TextInput {...form.getInputProps('requestId')}
                label='Generated Request ID'
                placeholder='Copy request ID here to use later'
                w="100%"
                readOnly
                rightSection={
                  <Button type='button' bg={(copyText === 'Copy') ? color.light[0] : color.disabled[0]} radius={2} onClick={handleCopy} h='100%' w='100%' p={0} m={0}>{copyText}</Button>
                }
                rightSectionWidth={75}
              />
            </Group>
            {
              !isSubmitted && (
                <Button bg={color.light[0]} type='submit' disabled={isDownloaded}>Start {downloadStatus === "completed" ? "Another" : ""} Download</Button>
              )
            }
            {
              (isSubmitted && downloadStatus !== 'completed') && (
                <>
                  <Button type='button' bg={color.light[0]} disabled={isCancelled || !requestId} onClick={cancelRequest}>Cancel</Button>
                  <ProgressBar progress={downloadProgress} message={progressBarMessage} style={{fillColor: color.light[0]}} />
                </>
              )
            }
            {
              isDownloaded && (
                <ProgressBar progress={downloadProgress} message={progressBarMessage} style={{fillColor: color.light[0]}} />
              )
            }
            {
              downloadStatus === "completed" && !isDownloaded && (
                <Button type='button' disabled={isDownloaded} bg={color.light[0]} onClick={() => downloadFile()}>Get File</Button>
              )
            }
            {
              apiError !== null && (
                <Center>
                  <Text style={{textAlign: 'center'}} c={color.light[0]}>ERROR: {apiError}</Text>
                </Center>
              )
            }
          </Flex>
        </form>
      </Flex>
      <Flex mb="100px" pl="10%" pr="10%" direction={isMobile ? "column" : "row"} justify={isMobile ? "center" : "space-around"} gap="lg">
        <Card w="300px" h="300px" p="40px" style={{borderRadius: "10px", boxShadow: "rgba(0, 0, 0, 0.35) 0px 5px 15px"}}>
          <Card.Section>
            <Flex direction="row" justify="center" align="center" gap="sm" mb="sm">
              <Title order={3}>About</Title>
            </Flex>
          </Card.Section>
          <Card.Section style={{overflowY: "auto"}}>
            <Flex direction="column" gap="sm">
              <Text size="sm">A <strong>full-stack web app</strong> for downloading videos or audio from <strong>multiple platforms</strong>. Built with <strong>React</strong> and <strong>Spring Boot</strong>, it features a mobile-first frontend and a multi-threaded backend to handle concurrent processing. The backend implements a <strong>REST API</strong> interface over <strong>yt-dlp</strong>, exposing its functionality via HTTP. The API is open-source and free to use.</Text>
              <Text size="sm"><strong>Version:</strong> 1.0.1</Text>
              <Text size="sm"><strong>Built with:</strong> React + Spring Boot</Text>
              <Text size="sm"><strong>License:</strong> MIT</Text>
              <Text size="sm"><strong>GitHub:</strong> <a href="https://github.com/Scrappyz/Media-Downloader">View Repo</a></Text>
              <Text size="sm"><strong>Issues:</strong> <a href="https://github.com/Scrappyz/Media-Downloader/issues">Report Bug</a></Text>
            </Flex>
          </Card.Section>
        </Card>
        <Card w="300px" h="300px" p="40px" style={{borderRadius: "10px", boxShadow: "rgba(0, 0, 0, 0.35) 0px 5px 15px"}}>
          <Card.Section>
            <Flex direction="row" justify="center" align="center" gap="sm">
              <Title order={3}>How To Use</Title>
            </Flex>
          </Card.Section>
          <Card.Section style={{overflowY: "auto"}}>
            <ol style={{paddingLeft: "20px", lineHeight: "1.8"}}>
              <li><Text size="sm"><strong>Choose Media Type:</strong> Select Video, Video Only, or Audio Only</Text></li>
              <li><Text size="sm"><strong>Get the Link:</strong> Go to the site, click Share → Copy Link (not the browser URL)</Text></li>
              <li><Text size="sm"><strong>Paste URL:</strong> Click the Paste button or manually enter the link</Text></li>
              <li><Text size="sm"><strong>Set Quality:</strong> Choose your preferred quality and format</Text></li>
              <li><Text size="sm"><strong>Name the File (Optional):</strong> Enter a custom filename or leave blank for default</Text></li>
              <li><Text size="sm"><strong>Start Download:</strong> Click "Start Download" and wait for processing</Text></li>
              <li><Text size="sm"><strong>Monitor Progress:</strong> Watch the progress bar in real-time</Text></li>
              <li><Text size="sm"><strong>Save File:</strong> Click "Get File" when done</Text></li>
            </ol>
          </Card.Section>
        </Card>
        <Card w="300px" h="300px" p="40px" style={{borderRadius: "10px", boxShadow: "rgba(0, 0, 0, 0.35) 0px 5px 15px"}}>
          <Card.Section>
            <Flex direction="row" justify="center" align="center" gap="sm">
              <Title order={3}>Supported Sites</Title>
            </Flex>
          </Card.Section>
          <Card.Section style={{overflowY: "auto"}}>
            <Flex direction="row" justify="center" pr="lg">
              <ul>
                {
                  supportedSites.map((site, index) => <li key={index}>{site}</li>)
                }
              </ul>
            </Flex>
          </Card.Section>
        </Card>
      </Flex>
    </MantineProvider>
  );
}

export default App;