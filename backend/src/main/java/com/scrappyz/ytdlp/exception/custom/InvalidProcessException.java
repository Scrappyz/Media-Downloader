package com.scrappyz.ytdlp.exception.custom;

public class InvalidProcessException extends ApiException {
    
    public InvalidProcessException() {
        super("bad_request", "Could not find process");
    }

    public InvalidProcessException(String message) {
        super("bad_request", message);
    }

}
