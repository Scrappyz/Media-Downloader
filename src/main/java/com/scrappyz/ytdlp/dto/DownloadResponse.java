package com.scrappyz.ytdlp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DownloadResponse {
    private int error;
    private String errorMessage;
}
