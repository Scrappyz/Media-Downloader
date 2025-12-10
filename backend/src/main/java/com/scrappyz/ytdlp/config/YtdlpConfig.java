package com.scrappyz.ytdlp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "ytdlp")
@Setter @Getter
public class YtdlpConfig {
    
    private boolean autoUpdate;
    private boolean useCookies;
    private String browserCookies;
    private String jsRuntime;
    
    public boolean isAutoUpdate() {
        return autoUpdate;
    }
}
