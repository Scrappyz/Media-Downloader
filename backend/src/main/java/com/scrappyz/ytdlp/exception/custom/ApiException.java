package com.scrappyz.ytdlp.exception.custom;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApiException extends RuntimeException {
    
    protected String code;

    public ApiException() {}

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String code, String message) {
        super(message);
        this.code = code;
    }
}
