package com.scrappyz.ytdlp.controller;

import java.io.IOException;
import java.nio.file.Path;

import com.scrappyz.ytdlp.service.WSDownloadService;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/d")
@RequiredArgsConstructor
public class DownloadMediaController {
  
  private final WSDownloadService wsDownloadService;

  @GetMapping("/{requesId}")
  public ResponseEntity<Resource> downloadFileRequestId(@PathVariable String requesId) throws IOException  {
    Path filePath = Path.of(wsDownloadService.hmap.get(requesId));

    Resource resource = new UrlResource(filePath.toUri());

    if (!resource.exists()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok()
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + resource.getFilename() + "\"")
      .body(resource);
  }
}
