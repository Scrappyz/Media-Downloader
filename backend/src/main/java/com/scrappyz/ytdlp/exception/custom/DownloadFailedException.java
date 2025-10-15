package com.scrappyz.ytdlp.exception.custom;

public class DownloadFailedException extends ApiException {

    public DownloadFailedException() {
        super("download_failed", "Download failed unexpectedly");
    }

    public DownloadFailedException(String message) {
        super("download_failed", message);
    }

}
