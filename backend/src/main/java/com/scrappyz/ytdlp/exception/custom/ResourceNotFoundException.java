package com.scrappyz.ytdlp.exception.custom;

public class ResourceNotFoundException extends ApiException {
    
    public ResourceNotFoundException() {
        super("not_found", "Could not find resource");
    }

    public ResourceNotFoundException(String message) {
        super("not_found", message);
    }

}
