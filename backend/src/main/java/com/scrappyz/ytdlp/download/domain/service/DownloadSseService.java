package com.scrappyz.ytdlp.download.domain.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.download.domain.model.DownloadProgress;
import com.scrappyz.ytdlp.download.domain.model.SseStatus;

@Service
public class DownloadSseService {
    
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, DownloadProgress> progressMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, SseStatus> statusMap = new ConcurrentHashMap<>();

    public void addEmitter(String id, Long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);
        emitters.put(id, emitter);
        statusMap.put(id, SseStatus.ACTIVE);

        emitter.onCompletion(() -> {
            if(statusMap.get(id) != SseStatus.ERROR) {
                statusMap.put(id, SseStatus.COMPLETED);
            }
        });

        emitter.onError(throwable -> {
            statusMap.put(id, SseStatus.ERROR);
        });

        progressMap.put(id, new DownloadProgress(0, "pending", null));
    }

    public void addEmitter(String id) {
        addEmitter(id, 0L);
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
