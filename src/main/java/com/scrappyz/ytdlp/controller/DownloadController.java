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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResourceErrorResponse;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.dto.DownloadResult;
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

        if(DownloadService.ErrorCode.DENIED.getString().equals(response.getError())) {
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<DownloadResult> checkRequest(@PathVariable String requestId) {
        CompletableFuture<DownloadResult> future = downloadService.getProcess(requestId);
        DownloadResult result = new DownloadResult();

        result.setStatus("pending");

        if(future.isDone()) {
            result = future.getNow(result);
            downloadService.cancelProcess(requestId);
        }

        if(result.getStatus().equals("failed")) {
            return ResponseEntity.internalServerError()
                    .body(result);
        }

        return ResponseEntity.ok().body(result);
    }

    @GetMapping("/{requestId}/file")
    public ResponseEntity<Object> getResource(@PathVariable String requestId,
        @RequestParam(name = "output", required = false, defaultValue = "") String outputName) {

        HttpHeaders headers = new HttpHeaders();
        FileSystemResource resource;

        try {
            resource = downloadService.getResource(requestId);
        } catch(FileNotFoundException e) {
            DownloadResourceErrorResponse response = new DownloadResourceErrorResponse();
            response.setMessage(e.getMessage());

            return ResponseEntity.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }

        String filename = resource.getFilename();
        int extensionIndex = filename.lastIndexOf('.');
        String extension = filename.substring(extensionIndex);

        if(outputName.isEmpty()) {
            outputName = requestId;
        }

        outputName += extension;

        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + outputName);

        return ResponseEntity.ok().headers(headers).body(resource);
    }
    
}
