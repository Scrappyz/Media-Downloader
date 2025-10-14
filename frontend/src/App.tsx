import React, { useEffect, useRef, useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group, Text, Title, Input } from '@mantine/core';

import { api } from './globals';
import { useForm } from '@mantine/form';

import '@mantine/core/styles.css';

interface DownloadRequest {
  requestType: string | undefined,
  url: string,
  videoQuality?: number,
  audioCodec?: string,
  audioBitrate?: number
};

interface DownloadResponse {
  requestId: string
};

interface StatusResponse {
  status: string,
  message: string | null
};

function App() {

  const [apiError, setApiError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  const [downloadStatus, setDownloadStatus] = useState<string | null>(null);

  console.log("RequestID:", requestId);

  const mediaTypes: string[] = ["Video", "Video Only", "Audio Only"];
  const videoQualities: string[] = ["144p", "240p", "360p", "480p", "720p", "1080p", "2160p"];
  const audioCodecs: string[] = ["mp3", "m4a", "wav", "flac"];
  const pollInterval: number = 5000;

  const mediaTypeMap = new Map<string, string>([
    ["Video", "video"],
    ["Video Only", "video_only"],
    ["Audio Only", "audio_only"]
  ]);

  const form = useForm({
    mode: 'controlled',
    initialValues: {
      type: "Video",
      url: "",
      videoQuality: "480p",
      audioCodec: "m4a"
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
      const clipText = await navigator.clipboard.readText();
      return clipText;
    } catch(error) {
      return error;
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
      request.audioCodec = values.audioCodec;
    } else {
      request.videoQuality = parseInt(values.videoQuality);
    }

    return request;
  }

  const handleSubmit = async (values: FormValues): Promise<any> => {
    const request = transformRequest(values);

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

        if(!res.ok) {
          const txt = await res.text();
          throw new Error(`Status fetch failed: ${res.status} ${txt}`);
        }

        const body: StatusResponse = await res.json();

        if(!mounted.current) return;

        setDownloadStatus(body.status);

        if(body.status === "success" || body.status === "failed") {
          setIsPolling(false);
          return;
        }

        // schedule next poll
        timer = setTimeout(() => {
          if (!stopped) {
            pollStatus()
          };
        }, pollInterval);

      } catch (error: any) {
        if (error.name === "AbortError") {
          return;
        }

        // network or server error — show and retry after interval
        if (!mounted.current) return;
        setApiError(error?.message ?? "Polling error");

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

  const downloadFile = () => {
    if (!requestId) return;
    // either the status response provided a link: /api/v1/downloads/{id}/file
    const url = api + `/downloads/${encodeURIComponent(requestId)}/file`;
    window.open(url, "_blank");
  };

  return (
    <MantineProvider defaultColorScheme="dark">
      <Flex pl="10%" pr="10%" h="100vh" direction="column" justify="center" align="center" gap="lg">
        <Title order={2}>Youtube Downloader</Title>
        <form onSubmit={form.onSubmit((values) => handleSubmit(values))}>
          <Flex w='100%' direction="column" rowGap="lg">
            <NativeSelect {...form.getInputProps('type')} label='Type' withAsterisk key={form.key("type")} data={mediaTypes} />
            <Group gap="0" align='flex-end'>
              <TextInput {...form.getInputProps('url')}
                label='URL' withAsterisk key={form.key("url")} 
                placeholder='Enter video link here'
                rightSection={
                  <Button onClick={handlePaste} h='100%' w='100%' p={0} m={0}>Paste</Button>
                }
                rightSectionWidth={75}
              />
            </Group>
            {
              isVideo ? (
                <NativeSelect {...form.getInputProps('videoQuality')} label='Video Quality' withAsterisk key={form.key("videoQuality")} data={videoQualities} />
              ) : (
                <NativeSelect {...form.getInputProps('audioCodec')} label='Audio Codec' withAsterisk key={form.key("audioCodec")} data={audioCodecs} />
              )
            }
            <Button type='submit'>Submit</Button>
            {
              downloadStatus !== null ? (
                <Button onClick={() => downloadFile()}>Download</Button>
              ) : (
                <Button>Loading...</Button>
              )
            }
          </Flex>
        </form>
      </Flex>
    </MantineProvider>
  );
}

export default App;