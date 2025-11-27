package com.scrappyz.ytdlp.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "download")
@Getter @Setter
public class DownloadProperties {

    private float progressIncrement;
    private Duration timeout;
    
}
