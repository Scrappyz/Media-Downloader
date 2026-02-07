package com.scrappyz.ytdlp.download.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DownloadRequest {
    
    private String requestType;
    private String url;
    private String videoFormat;
    private String videoQuality;
    private String audioQuality;
    private String audioFormat;
    private boolean embedMetadata;

}
