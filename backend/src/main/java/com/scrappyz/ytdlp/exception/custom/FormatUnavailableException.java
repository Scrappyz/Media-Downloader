package com.scrappyz.ytdlp.exception.custom;

public class FormatUnavailableException extends ApiException {

    public FormatUnavailableException() {
        super("format_unavailable", "Could not find resource");
    }

    public FormatUnavailableException(String message) {
        super("format_unavailable", message);
    }

}
