package com.scrappyz.ytdlp.service;

import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.exception.custom.DownloadFailedException;

public interface DownloadProcessHandler<T> {

    T runProcess(List<String> commands,
                 String id,
                 SseEmitter emitter,
                 ProcessLineHandler processLineHandler,
                 ErrorLineHandler errorLineHandler) throws DownloadFailedException;

    @FunctionalInterface
    interface ProcessLineHandler {
        void handle(String line, SseEmitter emitter) throws Exception;
    }

    @FunctionalInterface
    interface ErrorLineHandler {
        YtdlpDownloadService.ErrorCode handle(String line) throws Exception;
    }
}