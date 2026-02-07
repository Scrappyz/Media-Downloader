package com.scrappyz.ytdlp.download.domain.model;

import com.scrappyz.ytdlp.download.domain.service.impl.YtdlpDownloadService.ErrorCode;

public class YtdlpProcessResult {
    
    private ErrorCode error;

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
    
}
