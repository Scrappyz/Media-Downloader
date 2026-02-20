package com.scrappyz.ytdlp.download.domain.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class DownloadProgress {
    
    private float percentage;
    private String status;

    public DownloadProgress(float percentage, String status) {
        this.percentage = percentage;
        this.status = status;
    }

}
