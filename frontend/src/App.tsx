import '@mantine/core/styles.css';
import { MantineProvider } from '@mantine/core';

import { Button } from '@mantine/core'
import './App.css'

function App() {

  return (
    <MantineProvider>
      <Button variant="filled">Button</Button>
    </MantineProvider>
  )
}

export default App
