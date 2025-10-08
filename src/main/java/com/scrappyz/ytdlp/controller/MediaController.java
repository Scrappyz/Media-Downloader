package com.scrappyz.ytdlp.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.model.DownloadResult;
import com.scrappyz.ytdlp.service.MediaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;
    
    @GetMapping("/print")
    public ResponseEntity<String> print() {
        return ResponseEntity.ok().body(MediaService.executablePath.toString());
    }

    @GetMapping("/download")
    public ResponseEntity<Object> download(@RequestBody DownloadRequest request) {
        DownloadResult result = mediaService.download(request.getUrl(), request.getRequestType(), request.getVideoQuality(),
                request.getAudioCodec(), request.getAudioBitrate(), request.getOutputName());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + request.getOutputName());

        if(result.getError() > 0) {
            DownloadResponse response = new DownloadResponse();
            return ResponseEntity.internalServerError()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(result.getResource());
    }

    // @GetMapping("/remove")
    // public String cleanTemp() {
    //     MediaDownloader.cleanDownloads();
    //     return "Success";
    // }
    
}
