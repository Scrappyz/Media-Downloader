package com.scrappyz.ytdlp.download.domain.exception.custom;

public class DownloadCancelledException extends ApiException {

    public DownloadCancelledException() {
        super("download_cancelled", "Download was cancelled by user");
    }

    public DownloadCancelledException(String message) {
        super("download_cancelled", message);
    }
    
}
