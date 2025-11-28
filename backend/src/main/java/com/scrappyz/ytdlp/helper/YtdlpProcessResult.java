package com.scrappyz.ytdlp.helper;

import com.scrappyz.ytdlp.service.DownloadHelper.ErrorCode;

public class YtdlpProcessResult {
    
    private String outputName;
    private ErrorCode error;

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }

    public boolean hasOutputName() {
        return outputName != null && !outputName.isEmpty();
    }
    
}
