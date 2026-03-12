package com.scrappyz.ytdlp.download.domain.exception.custom;

public class FailedProcessException extends ApiException {
    
    public FailedProcessException() {
        super("INTERNAL_ERROR", "The process failed unexpectedly");
    }

    public FailedProcessException(String message) {
        super("INTERNAL_ERROR", message);
    }

}
