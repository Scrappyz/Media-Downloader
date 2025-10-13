package com.scrappyz.ytdlp.exception.model;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    NOT_FOUND("not_found", HttpStatus.NOT_FOUND, "Resource not found"),
    BAD_REQUEST("bad_request", HttpStatus.BAD_REQUEST, "Invalid Request"),
    EXPIRED("expired", HttpStatus.GONE, "Resource expired"),
    FULL_QUEUE("full_queue", HttpStatus.TOO_MANY_REQUESTS, "Queue is full"),
    INTERNAL_ERROR("internal_error", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");

    public final String code;
    public final HttpStatus status;
    public final String defaultMessage;
    
    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

}
