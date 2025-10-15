package com.scrappyz.ytdlp.controller;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.DownloadCancelResponse;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.dto.DownloadResult;
import com.scrappyz.ytdlp.exception.custom.InvalidProcessException;
import com.scrappyz.ytdlp.service.DownloadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/downloads")
public class DownloadController {

    private final Logger log = LoggerFactory.getLogger(DownloadController.class);

    private final DownloadService downloadService;
    private final PathProperties paths;
    
    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        // log.info(paths.getExecutablePath().toString());
        return ResponseEntity.ok().body(paths.getDownloadPath().toString());
    }
    
    @PostMapping
    public ResponseEntity<DownloadResponse> download(@RequestBody DownloadRequest request) {
        DownloadResponse response = downloadService.enqueue(request);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<DownloadResult> checkRequest(@PathVariable String requestId) {
        CompletableFuture<DownloadResult> future = downloadService.getProcess(requestId);
        DownloadResult result = new DownloadResult();

        result.setStatus("pending");
        result.setMessage("Request is being processed");

        if(future.isDone()) {
            result = future.getNow(result);
            downloadService.removeProcess(requestId);
        }

        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/{requestId}/file")
    public ResponseEntity<FileSystemResource> getResource(@PathVariable String requestId,
        @RequestParam(name = "output", required = false, defaultValue = "") String outputName) {

        HttpHeaders headers = new HttpHeaders();
        FileSystemResource resource;

        resource = downloadService.getResource(requestId);

        String filename = resource.getFilename();
        int extensionIndex = filename.lastIndexOf('.');
        String extension = filename.substring(extensionIndex);

        if(outputName.isEmpty()) {
            outputName = requestId;
        }

        outputName += extension;

        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputName);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<DownloadCancelResponse> cancelDownload(@PathVariable String requestId) {
        DownloadCancelResponse response = new DownloadCancelResponse();

        response.setStatus("success");
        response.setMessage("Request was cancelled successfully");

        if(!downloadService.isProcessExist(requestId)) {
            throw new InvalidProcessException("Process with request ID \"" + requestId + "\" could not be found");
        }

        downloadService.cancelProcess(requestId);

        return ResponseEntity.ok().body(response);
    }
    
}
