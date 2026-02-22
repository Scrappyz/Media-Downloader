package com.scrappyz.ytdlp.download.domain.model;

public class YtdlpProcessResult {
    
    private DownloadErrorCode error;

    public DownloadErrorCode getError() {
        return error;
    }

    public void setError(DownloadErrorCode error) {
        this.error = error;
    }
    
}
