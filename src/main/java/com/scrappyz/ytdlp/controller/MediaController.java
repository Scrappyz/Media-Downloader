package com.scrappyz.ytdlp.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.model.DownloadResult;
import com.scrappyz.ytdlp.service.MediaService;

import lombok.RequiredArgsConstructor;

import com.github.f4b6a3.ulid.UlidCreator;

import com.scrappyz.ytdlp.utils.ThreadUtils;

import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequiredArgsConstructor
public class MediaController {

    @Autowired
    private final MediaService mediaService;
    
    @GetMapping("/print")
    public ResponseEntity<String> print(@Value("${server.servlet.context-path}") String temp) {
        temp = UlidCreator.getMonotonicUlid().toString();
        return ResponseEntity.ok().body(temp);
    }

    @GetMapping("/check/{processId}")
    public String checkProcess(@PathVariable String processId) {
        CompletableFuture<DownloadResult> f = mediaService.getProcess(processId);
        return (f.isDone() ? "Done" : "Still Running");
    }

    @GetMapping("/download")
    public ResponseEntity<String> download(@RequestBody DownloadRequest request) {
        String id = mediaService.enqueue(request);
        return ResponseEntity.ok(id);

        // HttpHeaders headers = new HttpHeaders();
        // headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + request.getOutputName());

        // if(result.getError() > 0) {
        //     DownloadResponse response = new DownloadResponse();
        //     return ResponseEntity.internalServerError()
        //             .headers(headers)
        //             .contentType(MediaType.APPLICATION_JSON)
        //             .body(response);
        // }
        
        // return ResponseEntity.ok()
        //         .headers(headers)
        //         .contentType(MediaType.APPLICATION_OCTET_STREAM)
        //         .body(result.getResource());
    }

    // @GetMapping("/remove")
    // public String cleanTemp() {
    //     MediaDownloader.cleanDownloads();
    //     return "Success";
    // }
    
}
