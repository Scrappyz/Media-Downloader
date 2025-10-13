import React from 'react';
import { MantineProvider, Button } from '@mantine/core';

import { api } from './globals';

function App() {
  const getData = async(): Promise<any> => {
    try {
      const result = await fetch(api + "/downloads/HJGEOGH");
      console.log(await result.json());
    } catch(error) {
      console.log(error);
    }
  }

  return (
    <MantineProvider theme={{ colorScheme: 'dark' }}>
      <Button onClick={() => getData()}>Panag</Button>
    </MantineProvider>
  );
}

export default App;