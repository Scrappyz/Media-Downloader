package com.scrappyz.ytdlp.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.utils.MediaDownloader;

@RestController
public class ApiController {
    
    @GetMapping("/print")
    public String print() {
        return MediaDownloader.executablePath.toString();
    }

    @GetMapping("/download")
    public ResponseEntity<DownloadResponse> download(@RequestBody DownloadRequest request) {
        DownloadResponse response = new DownloadResponse();
        response.setVideoQuality(request.getVideoQuality());
        response.setAudioCodec(request.getAudioCodec());
        response.setOutputName(request.getOutputName());

        response.setDownloadResponse(MediaDownloader.download(request.getUrl(), request.getRequestType(), request.getVideoQuality(),
                request.getAudioCodec(), request.getAudioBitrate(), request.getOutputName()));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    
}
