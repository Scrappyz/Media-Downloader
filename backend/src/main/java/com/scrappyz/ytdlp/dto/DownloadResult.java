package com.scrappyz.ytdlp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DownloadResult {
    
    String status; // success | failed | queued | pending | invalid
    String message;

}
