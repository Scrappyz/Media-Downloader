package com.scrappyz.ytdlp.download.domain.exception.custom;

public class DownloadFailedException extends ApiException {

    public DownloadFailedException() {
        super("DOWNLOAD_FAILED", "Download failed unexpectedly");
    }

    public DownloadFailedException(String message) {
        super("DOWNLOAD_FAILED", message);
    }

}
