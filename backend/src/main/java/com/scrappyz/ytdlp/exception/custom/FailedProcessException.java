package com.scrappyz.ytdlp.exception.custom;

public class FailedProcessException extends ApiException {
    
    public FailedProcessException() {
        super("internal_error", "The process failed unexpectedly");
    }

    public FailedProcessException(String message) {
        super("internal_error", message);
    }

}
