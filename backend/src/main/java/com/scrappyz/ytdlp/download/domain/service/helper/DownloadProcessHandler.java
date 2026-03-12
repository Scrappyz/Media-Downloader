package com.scrappyz.ytdlp.download.domain.service.helper;

import java.util.List;

import com.scrappyz.ytdlp.download.domain.enums.DownloadErrorCode;
import com.scrappyz.ytdlp.download.domain.exception.custom.DownloadFailedException;

public interface DownloadProcessHandler<T> {

    T runProcess(List<String> commands,
                 String id,
                 ProcessLineHandler processLineHandler,
                 ErrorLineHandler errorLineHandler) throws DownloadFailedException;

    @FunctionalInterface
    interface ProcessLineHandler {
        DownloadErrorCode handle(String line, String id) throws Exception;
    }

    @FunctionalInterface
    interface ErrorLineHandler {
        DownloadErrorCode handle(String line) throws Exception;
    }
}