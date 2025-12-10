import { useEffect, useRef, useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group, Text, Title, Loader, Center } from '@mantine/core';

import { api } from './globals';
import { useForm } from '@mantine/form';

import '@mantine/core/styles.css';
import { parseFilenameFromContentDisposition } from './utils';

import { color } from './themes';

import { useDownloadProgress } from './hooks/useDownloadProgress';
import ProgressBar from './components/ProgressBar';

interface DownloadRequest {
  requestType: string | undefined,
  url: string,
  videoQuality?: string | number,
  videoFormat?: string,
  audioQuality?: string | number,
  audioFormat?: string,
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

function App() {

  const [apiError, setApiError] = useState<string | null>(null);
  const [requestId, setRequestId] = useState<string | null>(null);
  const [downloadStatus, setDownloadStatus] = useState<string | null>(null);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isDownloaded, setIsDownloaded] = useState(false);
  const [isCancelled, setIsCancelled] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState(0);
  const { status, progress } = useDownloadProgress({requestId: requestId || "", url: requestId ? (api + `/downloads/${encodeURIComponent(requestId)}`) : null});

  const mediaTypes: string[] = ["Video", "Video Only", "Audio Only"];
  const videoQualities: string[] = ["2160p", "1440p", "1080p", "720p", "480p", "360p", "240p", "144p"];
  const videoFormats: string[] = ["mp4"];
  const audioQualities: string[] = ["320kbps", "256kbps", "192kbps", "128kbps"];
  const audioFormats: string[] = [];

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

      if(values.audioQuality !== "Best" && values.audioQuality !== "Worse") {
        request.audioQuality = parseInt(values.audioQuality);
      } else {
        request.audioQuality = values.audioQuality;
      }
    } else {
      if(values.videoFormat !== "Default") {
        request.videoFormat = values.videoFormat;
      }

      if(values.videoQuality !== "Best" && values.videoQuality !== "Worst") {
        request.videoQuality = parseInt(values.videoQuality);
      } else {
        request.videoQuality = values.videoQuality;
      }
    }

    return request;
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

  const handleSubmit = async (values: FormValues): Promise<any> => {
    if(isSubmitted) {
      return;
    }

    setIsDownloaded(false);
    setIsSubmitted(true);
    setApiError(null);
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

      if(!response.ok) {
        throw new Error(`Response status: ${response.status}`);
      }

      const data = await response.json();
      setRequestId(data.requestId);
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

      if(data.status === "success") {
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
        method: "GET",
        headers: { "Accept": "application/octet-stream" }
      });

      if(!response.ok) {
        const res: ApiError = await response.json();
        throw new Error(res.message);
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
        // console.log(`Received ${receivedLength} of ${contentLength}: ${percentage}%`);
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

    if(status !== "success") return;

    setDownloadStatus(status);
    setIsSubmitted(false);
  }, [status]);

  useEffect(() => {
    setDownloadProgress(prev => prev = progress);
  }, [progress])

  let progressBarMessage = (downloadProgress > 0) ? `Downloading: ${downloadProgress}%` : "Pending...";
  if(isDownloaded) { // 2nd phase of download (file download)
    progressBarMessage = (downloadProgress > 0) ? `Uploading: ${downloadProgress}%` : "Uploading to user...";
  }

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
                  <NativeSelect w='45%' {...form.getInputProps('videoQuality')} label='Video Quality' withAsterisk key={form.key("videoQuality")} data={["Best", ...videoQualities, "Worst"]} />
                  <NativeSelect w='45%' {...form.getInputProps('videoFormat')} label='Video Format' withAsterisk key={form.key("videoFormat")} data={["Default", ...videoFormats]} />
                </Group>
              ) : (
                <Group justify='space-between'>
                  <NativeSelect w='45%' {...form.getInputProps('audioQuality')} label='Audio Quality' withAsterisk key={form.key("audioQuality")} data={["Best", ...audioQualities, "Worse"]} />
                  <NativeSelect w='45%' {...form.getInputProps('audioFormat')} label='Audio Format' withAsterisk key={form.key("audioFormat")} data={["Default", ...audioFormats]} />
                </Group>
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
                  <ProgressBar progress={downloadProgress} message={progressBarMessage} style={{fillColor: color.light[0]}} />
                </>
              )
            }
            {
              (isDownloaded) && (
                <ProgressBar progress={downloadProgress} message={progressBarMessage} style={{fillColor: color.light[0]}} />
              )
            }
            {
              downloadStatus === "success" && !isDownloaded && (
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