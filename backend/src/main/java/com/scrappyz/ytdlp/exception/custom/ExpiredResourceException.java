package com.scrappyz.ytdlp.exception.custom;

public class ExpiredResourceException extends ApiException {
    
    public ExpiredResourceException() {
        super("resource_expired", "The resource has expired");
    }

    public ExpiredResourceException(String message) {
        super("resource_expired", message);
    }

}
