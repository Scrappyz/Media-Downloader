package com.scrappyz.ytdlp.download.domain.exception.custom;

public class ResourceNotFoundException extends ApiException {
    
    public ResourceNotFoundException() {
        super("RESOURCE_NOT_FOUND", "Could not find resource");
    }

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

}
