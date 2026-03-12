package com.scrappyz.ytdlp.download.domain.exception.custom;

public class DownloadCancelledException extends ApiException {

    public DownloadCancelledException() {
        super("DOWNLOAD_CANCELLED", "Download was cancelled by user");
    }

    public DownloadCancelledException(String message) {
        super("DOWNLOAD_CANCELLED", message);
    }
    
}
