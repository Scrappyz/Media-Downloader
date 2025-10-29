import { useEffect, useRef, useState } from 'react';
import { MantineProvider, Button, Flex, NativeSelect, TextInput, Group, Text, Title, Loader, Center } from '@mantine/core';

import { useForm } from '@mantine/form';

import '@mantine/core/styles.css';
import { parseFilenameFromContentDisposition } from './utils';

import { color } from './themes';

import SockJs from "sockjs-client";
import { Client } from '@stomp/stompjs';

const address: string = window.location.protocol + "//" + window.location.hostname;
let api: string = address;

if (window.location.hostname.indexOf("localhost") > -1 || window.location.hostname.indexOf("192.168.") > -1) {
  api += ":3000";
}
api += "/api/v1";

function App() {

  const socket = new SockJs(api + "/ws");
  let stompClient = new Client({
    webSocketFactory: () => socket,
    debug: (str) => console.log(str),
    onConnect: () => {
      console.log("Connected!");

      stompClient.subscribe("/topic/helloworld", function (msg) {
        console.log(msg.body);
      })

      stompClient.publish({
        destination: "/app/wsdownload",
        body: JSON.stringify({
          requestType: "video",
          url: "https://youtu.be/wJnBTPUQS5A?si=EmjiSRYVNyHZnZkf",
          videoFormat: "default",
          videoQuality: "720",
          audioFormat: "default",
          id: "helloworld"
        })
      });
    }
  });

  stompClient.activate();

  return (
    <div>

    </div>
  )
}

export default App;