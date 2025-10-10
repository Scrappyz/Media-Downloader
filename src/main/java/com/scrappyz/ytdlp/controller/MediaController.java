package com.scrappyz.ytdlp.controller;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResourceErrorResponse;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.model.DownloadResult;
import com.scrappyz.ytdlp.service.MediaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/download")
public class MediaController {

    private final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final MediaService mediaService;
    private final PathProperties paths;
    
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        // log.info(paths.getExecutablePath().toString());
        return ResponseEntity.ok().body(paths.getDownloadPath().toString());
    }

    @GetMapping
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
            mediaService.cancelProcess(processId);
        }

        if(result.getStatus().equals("failed")) {
            return ResponseEntity.internalServerError()
                    .body(result);
        }

        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/get/{resourceName}")
    public ResponseEntity<Object> getResource(@PathVariable String resourceName,
        @RequestParam(name = "output", required = false, defaultValue = "") String outputName) {

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

        if(outputName.isEmpty()) {
            outputName = resourceName;
        }

        outputName += extension;

        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputName);

        return ResponseEntity.ok().headers(headers).body(resource);
    }
    
}
