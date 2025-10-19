package com.scrappyz.ytdlp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "ytdlp")
@Getter @Setter
public class YtdlpConfig {
    
    private boolean autoUpdate;
    
    public boolean isAutoUpdate() {
        return autoUpdate;
    }
}
