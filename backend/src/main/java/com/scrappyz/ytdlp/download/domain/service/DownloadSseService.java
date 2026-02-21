package com.scrappyz.ytdlp.download.domain.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.download.domain.model.DownloadProgress;

@Service
public class DownloadSseService {
    
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, DownloadProgress> progressMap = new ConcurrentHashMap<>();

    public void addEmitter(String id, SseEmitter emitter) {
        emitters.put(id, emitter);
        progressMap.put(id, new DownloadProgress(0, "pending", null));
    }

    public void removeEmitter(String id) {
        emitters.get(id).complete();
        emitters.remove(id);
        progressMap.remove(id);
    }

    public SseEmitter getEmitter(String id) {
        return emitters.get(id);
    }

    public DownloadProgress getProgress(String id) {
        return progressMap.get(id);
    }

    public void setProgress(String id, float percentage, String status, String message) {
        DownloadProgress progress = progressMap.get(id);
        if(progress != null) {
            progress.setPercentage(percentage);
            progress.setStatus(status);
            progress.setMessage(message);
        }
    }
}
