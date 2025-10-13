import React, { useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group, Text, Title } from '@mantine/core';

import { api } from './globals';
import { useForm } from '@mantine/form';

import '@mantine/core/styles.css';

function App() {
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

  const mediaTypes: string[] = ["Video", "Video Only", "Audio Only"];
  const videoQualities: string[] = ["144p", "240p", "360p", "480p", "720p", "1080p", "2160p"];
  const audioCodecs: string[] = ["mp3", "m4a", "wav", "flac"];

  const mediaTypeMap = new Map<string, string>([
    ["Video", "video"],
    ["Video Only", "video_only"],
    ["Audio Only", "audio_only"]
  ]);

  const [mediaType, setMediaType] = useState(mediaTypes[0]);
  const [url, setUrl] = useState("");

  const isVideo: boolean = (mediaType === "Video" || mediaType === "Video Only");

  const form = useForm({
    mode: 'uncontrolled',
    initialValues: {
      type: "Video",
      url: "",
      videoQuality: "480p",
      audioCodec: "m4a"
    }
  });

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
    setMediaType(val);
  }

  const handleUrlChange = (e) => {
    const val = e.target.value;
    // form.setFieldValue("url", val);
    setUrl(val);
  }

  const handlePaste = () => {
    getClipboardText().then(res => {
      console.log("Clipboard:", res);
      form.setFieldValue("url", res);
      setUrl(res);
    })
  }

  const transformRequest = (values: any): DownloadRequest => {
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

  const makeDownloadRequest = async (values): Promise<any> => {
    const request: DownloadRequest = transformRequest(values);
    
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
      return data;
    } catch(error) {
      console.error(error);
      return error;
    }
  }

  const handleSubmit = (values) => {
    console.log("Values:", values);
    makeDownloadRequest(values).then(res => {
      console.log(res);
    })
  }

  return (
    <MantineProvider defaultColorScheme="dark">
      <Flex pl="10%" pr="10%" h="100vh" direction="column" justify="center" align="center" gap="lg">
        <Title order={2}>Youtube Downloader</Title>
        <form onSubmit={form.onSubmit((values) => handleSubmit(values))}>
          <Flex direction="column" rowGap="lg">
            <NativeSelect label='Type' withAsterisk key={form.key("type")} value={mediaType} data={mediaTypes} onChange={handleTypeChange} />
            <Group gap="0">
              <TextInput label='URL' withAsterisk key={form.key("url")} value={url} placeholder='Enter video link here' radius="0" onChange={handleUrlChange} />
              <Button mt='25px' onClick={handlePaste} radius="0">Paste</Button>
            </Group>
            {
              isVideo ? (
                <NativeSelect label='Video Quality' withAsterisk key={form.key("videoQuality")} data={videoQualities} />
              ) : (
                <NativeSelect label='Audio Codec' withAsterisk key={form.key("audioCodec")} data={audioCodecs} />
              )
            }
            <Button type='submit'>Submit</Button>
          </Flex>
        </form>
        {/* <Button onClick={() => console.log(form.getValues())}>Display Values</Button> */}
      </Flex>
    </MantineProvider>
  );
}

export default App;