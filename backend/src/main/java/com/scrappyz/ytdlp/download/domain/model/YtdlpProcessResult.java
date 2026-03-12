package com.scrappyz.ytdlp.download.domain.model;

import com.scrappyz.ytdlp.download.domain.enums.DownloadErrorCode;

public class YtdlpProcessResult {
    
    private DownloadErrorCode error;

    public DownloadErrorCode getError() {
        return error;
    }

    public void setError(DownloadErrorCode error) {
        this.error = error;
    }
    
}
