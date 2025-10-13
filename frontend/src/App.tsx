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

  const [url, setUrl] = useState("");

  const form = useForm({
    mode: 'uncontrolled',
    initialValues: {
      type: "Video",
      url: ""
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
            <NativeSelect w="" key={form.key("type")} data={["Video", "Video Only", "Audio Only"]} />
            <Group gap="0">
              <TextInput key={form.key("url")} value={url} placeholder='Enter video link here' radius="0" onChange={(e) => setUrl(e.target.value)} />
              <Button onClick={handlePaste} radius="0">Paste</Button>
            </Group>
          </Flex>
        </form>
      </Flex>
    </MantineProvider>
  );
}

export default App;