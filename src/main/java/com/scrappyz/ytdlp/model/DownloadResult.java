package com.scrappyz.ytdlp.model;

import java.util.List;

import org.springframework.core.io.Resource;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DownloadResult {
    
    String status;
    String errorMessage;
    String downloadResourceName;

}
