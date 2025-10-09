package com.scrappyz.ytdlp.controller;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResourceErrorResponse;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.model.DownloadResult;
import com.scrappyz.ytdlp.service.MediaService;

import lombok.RequiredArgsConstructor;

import com.github.f4b6a3.ulid.UlidCreator;

import com.scrappyz.ytdlp.utils.ThreadUtils;

import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequiredArgsConstructor
@RequestMapping("/download")
public class MediaController {

    @Autowired
    private final MediaService mediaService;
    
    @GetMapping("/print")
    public ResponseEntity<String> print(@Value("${server.servlet.context-path}") String temp) {
        temp = UlidCreator.getMonotonicUlid().toString();
        return ResponseEntity.ok().body(temp);
    }

    @GetMapping("/")
    public ResponseEntity<DownloadResponse> download(@RequestBody DownloadRequest request) {
        String id = mediaService.enqueue(request);
        DownloadResponse response = new DownloadResponse();
        response.setRequestId(id);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/check/{processId}")
    public ResponseEntity<DownloadResult> checkRequest(@PathVariable String processId) {
        CompletableFuture<DownloadResult> future = mediaService.getProcess(processId);
        DownloadResult result = new DownloadResult();

        result.setStatus("pending");

        if(future.isDone()) {
            result = future.getNow(result);
        }

        if(result.getStatus().equals("failed")) {
            return ResponseEntity.internalServerError()
                    .body(result);
        }

        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/get/{resourceName}")
    public ResponseEntity<Object> getResource(@PathVariable String resourceName,
        @RequestParam(name = "output", required = false) String outputName) {

        HttpHeaders headers = new HttpHeaders();
        FileSystemResource resource;

        try {
            resource = mediaService.getResource(resourceName);
        } catch(FileNotFoundException e) {
            DownloadResourceErrorResponse response = new DownloadResourceErrorResponse();
            response.setErrorMessage(e.getMessage());

            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }

        int extensionIndex = resourceName.lastIndexOf('.');
        String extension = resourceName.substring(extensionIndex);

        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputName + extension);

        return ResponseEntity.ok().headers(headers).body(resource);
    }
    
}
