package com.scrappyz.ytdlp.download.domain.exception.custom;

public class FullDownloadQueueException extends ApiException {
    
    public FullDownloadQueueException() {
        super("FULL_DOWNLOAD_QUEUE", "Download queue is full");
    }

    public FullDownloadQueueException(String message) {
        super("FULL_DOWNLOAD_QUEUE", message);
    }

}
