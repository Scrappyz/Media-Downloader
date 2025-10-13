package com.scrappyz.ytdlp.exception.custom;

public class UnsupportedUrlException extends ApiException {
    
    public UnsupportedUrlException() {
        super("unsupported_url", "The given URL is not supported");
    }

    public UnsupportedUrlException(String message) {
        super("unsupported_url", message);
    }

}
