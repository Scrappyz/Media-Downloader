package com.scrappyz.ytdlp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.f4b6a3.ulid.Ulid;
import com.scrappyz.ytdlp.dto.DownloadCancelResponse;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.exception.custom.InvalidUlidException;
import com.scrappyz.ytdlp.service.DownloadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/downloads")
public class DownloadController {

    private final Logger log = LoggerFactory.getLogger(DownloadController.class);

    @Qualifier("ytdlp")
    private final DownloadService downloadService;
    
    @PostMapping
    public ResponseEntity<DownloadResponse> download(@RequestBody DownloadRequest request) {
        DownloadResponse response = downloadService.enqueue(request);

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{requestId}")
    public SseEmitter checkRequest(@PathVariable String requestId) {
        if(!Ulid.isValid(requestId)) {
            throw new InvalidUlidException();
        }

        return downloadService.getEmitter(requestId); // Subscribe to SSE events for this request
    }

    @GetMapping("/{requestId}/file")
    public ResponseEntity<FileSystemResource> getResource(@PathVariable String requestId,
        @RequestParam(name = "output", required = false, defaultValue = "") String outputName) {

        if(!Ulid.isValid(requestId)) {
            throw new InvalidUlidException();
        }

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

        String contentAttachment = String.format("attachment; filename=\"%s\"", outputName);
        log.info("[DownloadController.getResource] Returning content disposition with '" + contentAttachment + "'");

        headers.add(HttpHeaders.CONTENT_DISPOSITION, contentAttachment);
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        headers.add(HttpHeaders.CONTENT_LENGTH, Long.toString(resource.getFile().length()));

        log.info("[DownloadController.getResource] Returning content with length " + resource.getFile().length());

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<DownloadCancelResponse> cancelDownload(@PathVariable String requestId) {
        
        if(!Ulid.isValid(requestId)) {
            throw new InvalidUlidException();
        }

        DownloadCancelResponse response = new DownloadCancelResponse();

        response.setStatus("success");
        response.setMessage("Request was cancelled successfully");

        downloadService.cancelDownload(requestId);

        return ResponseEntity.ok().body(response);
    }
    
}
