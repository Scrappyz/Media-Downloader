package com.scrappyz.ytdlp.download.domain.exception.custom;

public class UnsupportedUrlException extends ApiException {
    
    public UnsupportedUrlException() {
        super("UNSUPPORTED_URL", "The given URL is not supported");
    }

    public UnsupportedUrlException(String message) {
        super("UNSUPPORTED_URL", message);
    }

}
