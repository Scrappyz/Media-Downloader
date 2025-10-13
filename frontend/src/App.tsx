import React, { useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group, Text, Title } from '@mantine/core';

import { api } from './globals';
import { useForm } from '@mantine/form';

import '@mantine/core/styles.css';

function App() {
  // const getData = async(): Promise<any> => {
  //   try {
  //     const result = await fetch(api + "/downloads/HJGEOGH");
  //     console.log(await result.json());
  //   } catch(error) {
  //     console.log(error);
  //   }
  // }

  const mediaTypes: string[] = ["Video", "Video Only", "Audio Only"];
  const videoQualities: string[] = ["144p", "240p", "360p", "480p", "720p", "1080p", "2160p"];
  const audioCodecs: string[] = ["mp3", "m4a", "wav", "flac"];

  const [mediaType, setMediaType] = useState(mediaTypes[0]);
  const [url, setUrl] = useState("");

  const isVideo: boolean = (mediaType === "Video" || mediaType === "Video Only");
  const isAudio: boolean = mediaType === "Audio Only";

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

  const handlePaste = () => {
    getClipboardText().then(res => {
      console.log("Clipboard:", res);
      form.setFieldValue("url", res);
      setUrl(res);
    })
  }

  return (
    <MantineProvider defaultColorScheme="dark">
      <Flex pl="10%" pr="10%" h="100vh" direction="column" justify="center" align="center" gap="lg">
        <Title order={2}>Youtube Downloader</Title>
        <form onSubmit={form.onSubmit((values) => console.log(values))}>
          <Flex direction="column" rowGap="lg">
            <NativeSelect label='Type' withAsterisk key={form.key("type")} value={mediaType} data={mediaTypes} onChange={handleTypeChange} />
            <Group gap="0">
              <TextInput label='URL' withAsterisk key={form.key("url")} value={url} placeholder='Enter video link here' radius="0" onChange={(e) => setUrl(e.target.value)} />
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