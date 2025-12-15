package com.scrappyz.ytdlp.helper;

import com.scrappyz.ytdlp.service.YtdlpDownloadService.ErrorCode;

public class YtdlpProcessResult {
    
    private ErrorCode error;

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
    
}
