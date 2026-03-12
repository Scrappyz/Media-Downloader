package com.scrappyz.ytdlp.download.domain.exception.custom;

public class InvalidUrlException extends ApiException {
    
    public InvalidUrlException() {
        super("INVALID_URL", "The given URL is invalid");
    }

    public InvalidUrlException(String message) {
        super("INVALID_URL", message);
    }

}
