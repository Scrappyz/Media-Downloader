package com.scrappyz.ytdlp.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.scrappyz.ytdlp.dto.DownloadChunkRequest;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.WSPostRequestResult;
import com.scrappyz.ytdlp.service.WSDownloadService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WSDownloadController {
  private final WSDownloadService wsDownloadService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/wsdownload")
  public WSPostRequestResult getMediaInfo (@Payload DownloadRequest request) {
    WSPostRequestResult result = new WSPostRequestResult();
    wsDownloadService.openWebSocketConnection(request, messagingTemplate);
    result.setId(request.getId());
    return result;
  }

  @MessageMapping("/wschunk")
  public void downloadByChunk (@Payload DownloadChunkRequest request) {
    String fileName = wsDownloadService.hmap.get(request.getMediaId());
    System.out.println(request.getMediaId());
    System.out.println(wsDownloadService.hmap.get(request.getMediaId()));
    int chunkSize = request.getChunkSize();

    for (String str: wsDownloadService.hmap.keySet()) {
      System.out.println("Map: " + str + " " + wsDownloadService.hmap.get(str)) ;
    }

    System.out.println("Chunk size: " + chunkSize);
    
    File file = new File(fileName);
    long binSize = file.length();

    try {
      FileInputStream fis = new FileInputStream(fileName);
      byte[] buffer = new byte[chunkSize];
      int bytesRead;
      long i = 0;

      while ((bytesRead = fis.read(buffer)) != -1) {
        List<String> l = new ArrayList<>();
        String base64chunk = Base64.getEncoder().encodeToString(copyOf(buffer, bytesRead));
        l.add("Data");
        l.add(String.valueOf(i));
        l.add(String.valueOf(bytesRead));
        l.add(String.valueOf(binSize));
        l.add(base64chunk);

        Thread.sleep(100);
        messagingTemplate.convertAndSend("/topic/" + request.getId(), String.join(" ", l));

        i++;
      }
    }catch(Exception e) {

    }
  }

  public static byte[] copyOf(byte[] src, int len) {
    byte[] c = new byte[len];
    System.arraycopy(src, 0, c, 0, len);
    return c;
  }

}
