package com.scrappyz.ytdlp.download.domain.service.helper;

import java.util.List;

import com.scrappyz.ytdlp.download.domain.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.download.domain.service.impl.YtdlpDownloadService;

public interface DownloadProcessHandler<T> {

    T runProcess(List<String> commands,
                 String id,
                 ProcessLineHandler processLineHandler,
                 ErrorLineHandler errorLineHandler) throws DownloadFailedException;

    @FunctionalInterface
    interface ProcessLineHandler {
        YtdlpDownloadService.ErrorCode handle(String line, String id) throws Exception;
    }

    @FunctionalInterface
    interface ErrorLineHandler {
        YtdlpDownloadService.ErrorCode handle(String line) throws Exception;
    }
}