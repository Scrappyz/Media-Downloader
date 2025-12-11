package com.scrappyz.ytdlp.exception.custom;

public class InvalidUlidException extends ApiException {
    
    public InvalidUlidException() {
        super("invalid_ulid", "Invalid ULID provided");
    }

    public InvalidUlidException(String message) {
        super("invalid_ulid", message);
    }
    
}
