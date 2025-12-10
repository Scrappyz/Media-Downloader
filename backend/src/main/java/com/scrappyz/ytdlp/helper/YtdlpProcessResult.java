package com.scrappyz.ytdlp.helper;

import com.scrappyz.ytdlp.service.DownloadHelper.ErrorCode;

public class YtdlpProcessResult {
    
    private ErrorCode error;

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
    
}
