package com.scrappyz.ytdlp.model;

import org.springframework.core.io.Resource;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DownloadResult {
    
    int error;
    String message;
    Resource resource;

}
