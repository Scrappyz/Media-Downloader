package com.scrappyz.ytdlp.exception.custom;

public class DownloadFailedException extends ApiException {

    public DownloadFailedException() {
        super("internal_error", "Download failed unexpectedly");
    }

    public DownloadFailedException(String message) {
        super("internal_error", message);
    }

}
