package com.scrappyz.ytdlp.exception.custom;

public class InvalidUrlException extends ApiException {
    
    public InvalidUrlException() {
        super("invalid_url", "The given URL is invalid");
    }

    public InvalidUrlException(String message) {
        super("invalid_url", message);
    }

}
