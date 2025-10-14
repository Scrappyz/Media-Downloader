package com.scrappyz.ytdlp.exception.custom;

public class FormatUnavailableException extends ApiException {

    public FormatUnavailableException() {
        super("format_unavailable", "The requested format is unavailable");
    }

    public FormatUnavailableException(String message) {
        super("format_unavailable", message);
    }

}
