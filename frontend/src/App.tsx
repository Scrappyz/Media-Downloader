import React, { useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group } from '@mantine/core';

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

  const mediaTypes = ["Video", "Video Only", "Audio Only"];
  const videoQualities = ["144p", "240p", "360p", "480p", "720p", "1080p", "2160p"];

  const [url, setUrl] = useState("");

  const form = useForm({
    mode: 'uncontrolled',
    initialValues: {
      type: "Video",
      url: "",
      videoQuality: "480p"
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

  const handlePaste = () => {
    getClipboardText().then(res => {
      console.log("Clipboard:", res);
      form.setFieldValue("url", res);
      setUrl(res);
    })
  }

  return (
    <MantineProvider defaultColorScheme="dark">
      <Flex pl="10%" pr="10%" h="100vh" direction="row" justify="center" align="center">
        <form onSubmit={form.onSubmit((values) => console.log(values))}>
          <Flex direction="column" rowGap="lg">
            <NativeSelect key={form.key("type")} data={mediaTypes} />
            <Group gap="0">
              <TextInput key={form.key("url")} value={url} placeholder='Enter video link here' radius="0" onChange={(e) => setUrl(e.target.value)} />
              <Button onClick={handlePaste} radius="0">Paste</Button>
            </Group>
            <NativeSelect key={form.key("videoQuality")} data={videoQualities} />
          </Flex>
        </form>
        <Button onClick={() => console.log(form.getValues())}>Display Values</Button>
      </Flex>
    </MantineProvider>
  );
}

export default App;