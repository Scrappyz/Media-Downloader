package com.scrappyz.ytdlp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DownloadRequest {
    
    private String requestType;
    private String url;
    private int videoQuality;
    private String audioCodec = "";
    private int audioBitrate;
    private String outputName = "";

}
