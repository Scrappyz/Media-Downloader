package com.scrappyz.ytdlp.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "path")
@Validated
@Getter @Setter
public class PathProperties {
    
    private Path binPath;
    private Path ytdlpBin;
    private Path downloadPath;

    @PostConstruct
    public void init() {
        String bin = "";
        String os = System.getProperty("os.name", "unknown").toLowerCase();

        if(os.contains("win")) {
            bin = "yt-dlp.exe";
        } else if(os.contains("mac") || os.contains("darwin")) {
            bin = "yt-dlp_mac";
        } else {
            bin = "yt-dlp_linux";
        }

        ytdlpBin = ytdlpBin.resolve(bin).normalize();
    }
    
}
