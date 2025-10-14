import React, { useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group, Text, Title, Input } from '@mantine/core';

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

  const mediaTypes: string[] = ["Video", "Video Only", "Audio Only"];
  const videoQualities: string[] = ["144p", "240p", "360p", "480p", "720p", "1080p", "2160p"];
  const audioCodecs: string[] = ["mp3", "m4a", "wav", "flac"];

  const mediaTypeMap = new Map<string, string>([
    ["Video", "video"],
    ["Video Only", "video_only"],
    ["Audio Only", "audio_only"]
  ]);

  const type = form.getValues().type;
  const isVideo: boolean = (type === "Video" || type === "Video Only");

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

  const makeDownloadRequest = async (values: FormValues): Promise<any> => {
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
    console.log("Request:", transformRequest(values));
    // makeDownloadRequest(values).then(res => {
    //   console.log(res);
    // })
  }

  return (
    <MantineProvider defaultColorScheme="dark">
      <Flex pl="10%" pr="10%" h="100vh" direction="column" justify="center" align="center" gap="lg">
        <Title order={2}>Youtube Downloader</Title>
        <form onSubmit={form.onSubmit((values) => handleSubmit(values))}>
          <Flex direction="column" rowGap="lg">
            <NativeSelect {...form.getInputProps('type')} label='Type' withAsterisk key={form.key("type")} data={mediaTypes} />
            <Group gap="0" align='flex-end'>
              <TextInput {...form.getInputProps('url')}
                label='URL' withAsterisk key={form.key("url")} 
                placeholder='Enter video link here' 
                rightSection={
                  <Button onClick={handlePaste} h='100%' w='100%' p={0} m={0}>Paste</Button>
                }
                rightSectionWidth={80}
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
          </Flex>
        </form>
      </Flex>
    </MantineProvider>
  );
}

export default App;