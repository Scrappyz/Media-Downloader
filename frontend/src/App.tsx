import React, { useEffect, useRef, useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group, Text, Title, Input, Loader, Center } from '@mantine/core';

import { api } from './globals';
import { useForm } from '@mantine/form';

import '@mantine/core/styles.css';
import { parseFilenameFromContentDisposition } from './utils';

interface DownloadRequest {
  requestType: string | undefined,
  url: string,
  videoQuality?: number,
  videoFormat?: string,
  audioFormat?: string,
  outputName?: string
};

interface DownloadResponse {
  requestId: string
};

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
  const [isPolling, setIsPolling] = useState(false);
  const [downloadStatus, setDownloadStatus] = useState<string | null>(null);

  // console.log("RequestID:", requestId);

  const mediaTypes: string[] = ["Video", "Video Only", "Audio Only"];
  const videoQualities: string[] = ["144p", "240p", "360p", "480p", "720p", "1080p", "2160p"];
  const videoFormats: string[] = ["mp4", "mkv"];
  const audioFormats: string[] = ["mp3", "m4a", "wav", "flac"];
  const pollInterval: number = 5000;

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
      videoQuality: "480p",
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

  const getClipboardText = async(): Promise<string> => {
    try {
      if(!navigator?.clipboard || typeof navigator.clipboard.readText !== "function") {
        return "";
      }

      const clipText = await navigator.clipboard.readText();
      return String(clipText ?? "");
    } catch(error) {
      console.error("Clipboard read failed:", error);
      return "";
    }
  }

  const handleTypeChange = (e) => {
    const val = e.target.value;
    form.setFieldValue("type", val);
  }

  const handleUrlChange = (e) => {
    const val = e.target.value;
    form.setFieldValue("url", val);
  }

  const handlePaste = () => {
    getClipboardText().then(res => {
      console.log("Clipboard:", res);
      form.setFieldValue("url", res);
    })
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

  const handleSubmit = async (values: FormValues): Promise<any> => {
    setDownloadStatus(null);

    console.log("Form Values:", values);
    const request = transformRequest(values);
    console.log("Request Data:", request);

    try {
      const response = await fetch(api + "/downloads", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(`Response status: ${response.status}`);
      }

      const data = await response.json();
      setRequestId(data.requestId);
      setIsPolling(true);
      return data;
    } catch(error) {
      console.error(error);
      setApiError(error.message);
      return error;
    }
  }

  useEffect(() => {

    if(!requestId || !isPolling) {
      return;
    }

    let timer: number | null = null;
    let stopped = false;

    const pollStatus = async () => {
      // abort previous
      currentAbort.current?.abort();
      const ac = new AbortController();
      currentAbort.current = ac;

      try {
        const res = await fetch(api + `/downloads/${encodeURIComponent(requestId)}`, {
          method: "GET",
          headers: { "Accept": "application/json" },
          signal: ac.signal,
        });

        if(!res.ok) { // If it is not ok, stop polling
          const response: ApiError = await res.json();
          throw new Error(response.message);
        }

        const body: StatusResponse = await res.json();

        if(!mounted.current) return;

        setDownloadStatus(body.status);

        if(body.status === "success") {
          setIsPolling(false);
          return;
        }

        // schedule next poll
        timer = setTimeout(() => {
          if (!stopped) {
            pollStatus()
          };
        }, pollInterval);

      } catch(error: any) {

        if(error.name === "AbortError") {
          return;
        }

        setIsPolling(false);

        // network or server error — show and retry after interval
        if(!mounted.current) return;

        setApiError(error?.message ?? "Polling error");

        setDownloadStatus(null);

        timer = setTimeout(() => {
          if (!stopped) {
            pollStatus()
          };
        }, pollInterval);

      }
    };

    // initial immediate poll
    pollStatus();

    return () => {
      stopped = true;
      if (timer !== null) window.clearTimeout(timer);
      currentAbort.current?.abort();
    };

  }, [requestId, isPolling]);

  const reset = () => {
    setApiError(null);
    setRequestId(null);
    setIsPolling(false);
    setDownloadStatus(null);
  }

  const stopPolling = () => {
    setIsPolling(false);
    currentAbort.current?.abort();
  };

  const cancelRequest = async () => {
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
    if(!requestId) {
      return;
    }

    let url = api + `/downloads/${encodeURIComponent(requestId)}/file`;
    const outputName = form.getValues().outputName;

    if(outputName !== null && outputName.length > 0) {
      url += `?output=${encodeURIComponent(outputName)}`;
    }

    try {
      const response = await fetch(url, {
        method: "GET"
      });

      if(!response.ok) {
        const res: ApiError = await response.json();
        throw new Error(res.message);
      }

      const contentDisposition = response.headers.get("Content-Disposition");
      const filename = parseFilenameFromContentDisposition(contentDisposition);
      console.log("Filename:", filename);
      const blobUrl = window.URL.createObjectURL(await response.blob());
      const a = document.createElement("a");
      a.href = blobUrl;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => window.URL.revokeObjectURL(blobUrl), 10000);
    } catch(error: any) {
      setApiError(error.message);
    }
  };

  return (
    <MantineProvider defaultColorScheme="light">
      <Flex pl="10%" pr="10%" h="100vh" direction="column" justify="center" align="center" gap="lg">
        <Title order={2}>Youtube Downloader</Title>
        <form onSubmit={form.onSubmit((values) => handleSubmit(values))}>
        {/* <form onSubmit={(e) => {e.preventDefault; alert("submit"); form.onSubmit((values) => handleSubmit(values))}}> */}
          <Flex w='100%' direction="column" rowGap="lg">
            <NativeSelect {...form.getInputProps('type')} label='Type' withAsterisk key={form.key("type")} data={mediaTypes} />
            <Group gap="0" align='flex-end'>
              <TextInput {...form.getInputProps('url')}
                label='URL' withAsterisk key={form.key("url")} 
                placeholder='Enter video link here'
                // rightSection={
                //   <Button type='button' bg='red' radius={2} onClick={handlePaste} h='100%' w='100%' p={0} m={0}>Paste</Button>
                // }
                // rightSectionWidth={75}
              />
            </Group>
            {
              isVideo ? (
                <>
                  <NativeSelect {...form.getInputProps('videoQuality')} label='Video Quality' withAsterisk key={form.key("videoQuality")} data={videoQualities} />
                  <NativeSelect {...form.getInputProps('videoFormat')} label='Video Format' withAsterisk key={form.key("videoFormat")} data={["Default", ...videoFormats]} />
                </>
              ) : (
                <NativeSelect {...form.getInputProps('audioFormat')} label='Audio Format' withAsterisk key={form.key("audioFormat")} data={["Default", ...audioFormats]} />
              )
            }
            <TextInput {...form.getInputProps('outputName')}
              label='Output Name' withAsterisk key={form.key("outputName")} 
              placeholder='Enter video link here'
              rightSectionWidth={75}
            />
            {
              downloadStatus === null && (
                <Button bg='red' type='submit'>Submit</Button>
              )
            }
            {
              downloadStatus === null && apiError !== null && (
                <Text c='red'>{apiError}</Text>
              )
            }
            {
              downloadStatus === "pending" && (
                <>
                  <Button type='button' bg='red' onClick={cancelRequest}>Cancel</Button>
                  <Center>
                    <Loader color='red' />
                  </Center>
                </>
              )
            }
            {
              downloadStatus === "success" && (
                <>
                  <Button bg='red' type='submit'>Submit</Button>
                  <Button type='button' bg='red' onClick={() => downloadFile()}>Download</Button>
                </>
              )
            }
          </Flex>
        </form>
      </Flex>
    </MantineProvider>
  );
}

export default App;