package com.scrappyz.ytdlp.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.config.DownloadProperties;
import com.scrappyz.ytdlp.dto.DownloadProgressResponse;

import lombok.RequiredArgsConstructor;

@Component
@Scope("prototype") // Short lived helper instance per download
@RequiredArgsConstructor
public class DownloadProgressHelper {
    
    private static final Logger log = LoggerFactory.getLogger(DownloadHelper.class);
    private float lastProgressPercentage = 0;
    private final DownloadProperties downloadProperties;

    public void processLine(String line, SseEmitter emitter) {
        boolean isDownloadProgress = line.startsWith("[download]") && line.contains("%");
        float progressIncrement = downloadProperties.getProgressIncrement();

        if(!isDownloadProgress) {
            return;
        }

        int startIndex = "[download]".length() + 1;
        int endIndex = startIndex + 4; // 5 characters for XXX.X%

        StringBuilder progressStr = new StringBuilder(line.substring(startIndex, endIndex).trim());
        char lastChar = progressStr.charAt(progressStr.length() - 1);

        if(lastChar == '%' || lastChar == '.') {
            progressStr.deleteCharAt(progressStr.length() - 1);
        }

        // log.info("[DownloadHelper.processLine] Progress string: " + progressStr);
        float progress = Float.parseFloat(progressStr.toString());

        if(progress < lastProgressPercentage + progressIncrement || progress >= 100.0f) {
            return;
        }

        lastProgressPercentage = progress;

        log.info("[DownloadHelper.processLine] Progress: " + progress);

        DownloadProgressResponse progressResponse = new DownloadProgressResponse(progress);
        try {
            emitter.send(SseEmitter.event()
                .name("progress")
                .data(progressResponse)
            );
        } catch(IOException e) {
            log.info("[DownloadHelper.processLine] Failed to send progress update via SseEmitter");
        }
        
    }

}
