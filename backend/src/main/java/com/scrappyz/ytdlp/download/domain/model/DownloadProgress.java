package com.scrappyz.ytdlp.download.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class DownloadProgress {
    
    private float percentage;
    private String status;
    private String message;

    public DownloadProgress(float percentage, String status, String message) {
        this.percentage = percentage;
        this.status = status;
        this.message = message;
    }

}
