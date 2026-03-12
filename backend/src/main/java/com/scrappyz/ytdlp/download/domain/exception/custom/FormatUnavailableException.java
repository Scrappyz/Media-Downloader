package com.scrappyz.ytdlp.download.domain.exception.custom;

public class FormatUnavailableException extends ApiException {

    public FormatUnavailableException() {
        super("FORMAT_UNAVAILABLE", "The requested format is unavailable");
    }

    public FormatUnavailableException(String message) {
        super("FORMAT_UNAVAILABLE", message);
    }

}
