package com.scrappyz.ytdlp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DownloadRequest {
    
    private String requestType;
    private String url;
    private String videoFormat;
    private int videoQuality;
    private String audioFormat;

}
