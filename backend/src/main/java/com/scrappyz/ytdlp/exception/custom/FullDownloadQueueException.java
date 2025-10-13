package com.scrappyz.ytdlp.exception.custom;

public class FullDownloadQueueException extends ApiException {
    
    public FullDownloadQueueException() {
        super("too_many_requests", "Download queue is full");
    }

    public FullDownloadQueueException(String message) {
        super("too_many_requests", message);
    }

}
