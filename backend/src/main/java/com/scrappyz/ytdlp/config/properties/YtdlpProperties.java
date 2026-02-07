package com.scrappyz.ytdlp.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Setter;

@ConfigurationProperties(prefix = "ytdlp")
@Setter
public class YtdlpProperties {
    
    private boolean autoUpdate;
    private boolean useCookies;
    private String browserCookies;
    private String cookiesPath;
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

    public String getCookiesPath() {
        return cookiesPath;
    }

    public String getJsRuntime() {
        return jsRuntime;
    }
}
