package com.scrappyz.ytdlp.download.domain.exception.custom;

public class InvalidProcessException extends ApiException {
    
    public InvalidProcessException() {
        super("INVALID_PROCESS", "Could not find process");
    }

    public InvalidProcessException(String message) {
        super("INVALID_PROCESS", message);
    }

}
