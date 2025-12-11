package com.scrappyz.ytdlp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Setter;

@ConfigurationProperties(prefix = "ytdlp")
@Setter
public class YtdlpConfig {
    
    private boolean autoUpdate;
    private boolean useCookies;
    private String browserCookies;
    private String jsRuntime;
    
    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public boolean isUseCookies() {
        return useCookies;
    }

    public String getBrowserCookies() {
        return browserCookies;
    }

    public String getJsRuntime() {
        return jsRuntime;
    }
}
