package com.scrappyz.ytdlp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DownloadResponse {

    private String error; // null | denied
    private String message;
    private String requestId;

}
