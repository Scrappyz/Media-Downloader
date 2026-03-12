package com.scrappyz.ytdlp.download.domain.exception.custom;

public class ExpiredResourceException extends ApiException {
    
    public ExpiredResourceException() {
        super("RESOURCE_EXPIRED", "The resource has expired");
    }

    public ExpiredResourceException(String message) {
        super("RESOURCE_EXPIRED", message);
    }

}
