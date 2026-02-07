package com.scrappyz.ytdlp.download.domain.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadServiceFactory { // Use this once you add more beyond yt-dlp
    
    private final Map<String, DownloadService> services;

    public DownloadService get(String type) {
        DownloadService service = services.get(type);

        if(service == null) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }

        return service;
    }
}
