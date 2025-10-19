package com.scrappyz.ytdlp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "ytdlp")
@Getter @Setter
public class YtdlpConfig {
    
    private boolean autoUpdate;
    
    public boolean isAutoUpdate() {
        return autoUpdate;
    }
}
