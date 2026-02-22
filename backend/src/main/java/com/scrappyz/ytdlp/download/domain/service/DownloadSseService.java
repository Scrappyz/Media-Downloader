package com.scrappyz.ytdlp.download.domain.service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.download.api.dto.ApiError;
import com.scrappyz.ytdlp.download.api.dto.DownloadProgressResponse;
import com.scrappyz.ytdlp.download.api.dto.DownloadResult;
import com.scrappyz.ytdlp.download.domain.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.download.domain.exception.custom.FailedProcessException;
import com.scrappyz.ytdlp.download.domain.exception.custom.FormatUnavailableException;
import com.scrappyz.ytdlp.download.domain.exception.custom.InvalidUrlException;
import com.scrappyz.ytdlp.download.domain.exception.custom.UnsupportedUrlException;
import com.scrappyz.ytdlp.download.domain.model.DownloadErrorCode;
import com.scrappyz.ytdlp.download.domain.model.DownloadProgress;
import com.scrappyz.ytdlp.download.domain.model.SseStatus;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DownloadSseService {
    
    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, DownloadProgress> progressMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, SseStatus> statusMap = new ConcurrentHashMap<>();

    public void addEmitter(String id, Long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);
        emitters.put(id, emitter);
        statusMap.put(id, SseStatus.ACTIVE);
        progressMap.put(id, new DownloadProgress(0, "pending", null));

        emitter.onCompletion(() -> {
            if(statusMap.get(id) != SseStatus.ACTIVE) {
                return;
            }

            log.info("[DownloadSseService.addEmitter] SseEmitter with ID '" + id + "' has completed successfully");
            statusMap.put(id, SseStatus.COMPLETED);
        });

        emitter.onError(throwable -> {
            log.info("[DownloadSseService.addEmitter] SseEmitter with ID '" + id + "' had an error");
            statusMap.put(id, SseStatus.ERROR);
        });

        emitter.onTimeout(() -> {
            log.info("[DownloadSseService.addEmitter] SseEmitter with ID '" + id + "' reached timeout");
            statusMap.put(id, SseStatus.TIMEOUT);
        });
    }

    public void addEmitter(String id) {
        addEmitter(id, 0L);
    }

    public void removeEmitter(String id) {
        emitters.remove(id);
        progressMap.remove(id);
        statusMap.remove(id);
    }

    public SseEmitter getEmitter(String id) {
        return emitters.get(id);
    }

    public SseStatus getEmitterStatus(String id) {
        return statusMap.get(id);
    }

    public DownloadProgress getProgress(String id) {
        return progressMap.get(id);
    }

    public void completeEmitter(String id) {
        emitters.get(id).complete();
    }

    public void setProgress(String id, float percentage, String status, String message) {
        DownloadProgress progress = progressMap.get(id);
        if(progress != null) {
            progress.setPercentage(percentage);
            progress.setStatus(status);
            progress.setMessage(message);
        }
    }

    public void sendProgress(String id, float progress, String message) {
        SseEmitter emitter = emitters.get(id);
        DownloadProgressResponse response = new DownloadProgressResponse(progress, message);
        
        if(statusMap.get(id) == SseStatus.ERROR) {
            log.info("[DownloadSseService.sendProgress] SseEmitter with ID '" + id + "' could not send because it has already completed with an error");
            return;
        }

        if(statusMap.get(id) == SseStatus.TIMEOUT) {
            log.info("[DownloadSseService.sendProgress] SseEmitter with ID '" + id + "' could not send because it has reached timeout");
            return;
        }

        if(statusMap.get(id) == SseStatus.COMPLETED) {
            log.info("[DownloadSseService.sendProgress] SseEmitter with ID '" + id + "' could not send because it has already completed");
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                .name("progress")
                .data(response)
            );
        } catch(IOException ex) {
            log.info("[DownloadSseService.sendStatus] Failed to send download cancelled status via SseEmitter");
        }
    }

    public void sendStatus(String id, String status, String message) {
        SseEmitter emitter = emitters.get(id);
        DownloadResult result = new DownloadResult(status, message);
        
        if(statusMap.get(id) == SseStatus.ERROR) {
            log.info("[DownloadSseService.sendStatus] SseEmitter with ID '" + id + "' could not send because it has already completed with an error");
            return;
        }

        if(statusMap.get(id) == SseStatus.TIMEOUT) {
            log.info("[DownloadSseService.sendStatus] SseEmitter with ID '" + id + "' could not send because it has reached timeout");
            return;
        }

        if(statusMap.get(id) == SseStatus.COMPLETED) {
            log.info("[DownloadSseService.sendStatus] SseEmitter with ID '" + id + "' could not send because it has already completed");
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                .name("status")
                .data(result)
            );
        } catch(IOException ex) {
            log.info("[DownloadSseService.sendStatus] Failed to send due to IOException");
        }
    }

    public void sendError(String id, DownloadErrorCode error) 
        throws InvalidUrlException, UnsupportedUrlException, FormatUnavailableException, DownloadFailedException, FailedProcessException {
        
        SseEmitter emitter = emitters.get(id);
        try {
            if(error == DownloadErrorCode.INVALID_URL) {
                log.info("[DownloadSseService.sendError] Invalid URL");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.INVALID_URL.getString(), "The URL provided is not valid"))
                );
                throw new InvalidUrlException();
            }

            if(error == DownloadErrorCode.UNSUPPORTED_URL) {
                log.info("[DownloadSseService.sendError] Unsupported URL");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.UNSUPPORTED_URL.getString(), "The URL provided is not supported"))
                );

                throw new UnsupportedUrlException();
            }

            if(error == DownloadErrorCode.FORMAT_UNAVAILABLE) {
                log.info("[DownloadSseService.sendError] Format unavailable");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.FORMAT_UNAVAILABLE.getString(), "The format requested is unavailable"))
                );

                throw new FormatUnavailableException();
            }

            if(error == DownloadErrorCode.POSTPROCESSING_ERROR) {
                log.info("[DownloadSseService.sendError] Postprocessing error");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.POSTPROCESSING_ERROR.getString(), "There was a problem in postprocessing"))
                );

                throw new DownloadFailedException();
            }

            if(error == DownloadErrorCode.FAILED_UNEXPECTEDLY) {
                log.info("[DownloadSseService.sendError] Download has failed unexpectedly");
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data(new ApiError(DownloadErrorCode.FAILED_UNEXPECTEDLY.getString(), "Download has failed unexpectedly"))
                );

                throw new DownloadFailedException();
            }
        } catch(IOException e) {
            log.info("[DownloadSseService.sendStatus] Failed to send due to IOException");
        }
    }
}
