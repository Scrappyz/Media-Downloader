package com.scrappyz.ytdlp.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.utils.MediaDownloader;

import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class ApiController {
    
    @GetMapping("/print")
    public String print() {
        return MediaDownloader.executablePath.toString();
    }

    // @GetMapping("/download")
    // public ResponseEntity<DownloadResponse> download(@RequestBody DownloadRequest request) {
    //     DownloadResponse response = new DownloadResponse();
    //     response.setVideoQuality(request.getVideoQuality());
    //     response.setAudioCodec(request.getAudioCodec());
    //     response.setOutputName(request.getOutputName());

    //     response.setDownloadResponse(MediaDownloader.download(request.getUrl(), request.getRequestType(), request.getVideoQuality(),
    //             request.getAudioCodec(), request.getAudioBitrate(), request.getOutputName()));

    //     return ResponseEntity.status(HttpStatus.OK).body(response);
    // }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestBody DownloadRequest request) throws IOException {
        MediaDownloader.download(request.getUrl(), request.getRequestType(), request.getVideoQuality(),
                request.getAudioCodec(), request.getAudioBitrate(), request.getOutputName());

        Path path = MediaDownloader.downloadPath.resolve(request.getOutputName());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
        // MediaDownloader.logger.info(path.toString());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + request.getOutputName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/remove")
    public String cleanTemp() {
        MediaDownloader.cleanDownloads();
        return "Success";
    }
    
}
