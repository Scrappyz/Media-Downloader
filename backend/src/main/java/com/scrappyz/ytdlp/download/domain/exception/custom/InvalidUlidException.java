package com.scrappyz.ytdlp.download.domain.exception.custom;

public class InvalidUlidException extends ApiException {
    
    public InvalidUlidException() {
        super("INVALID_ULID", "Invalid ULID provided");
    }

    public InvalidUlidException(String message) {
        super("INVALID_ULID", message);
    }
    
}
