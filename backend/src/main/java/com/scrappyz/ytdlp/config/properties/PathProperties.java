package com.scrappyz.ytdlp.config.properties;

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
    
    private String ytdlpBinName;
    private Path ytdlpBinPath;
    private Path downloadPath;

    @PostConstruct
    public void init() {
        String bin;
        
        String os = System.getProperty("os.name", "unknown").toLowerCase();

        if(os.contains("win")) {
            bin = String.format("windows/%s.exe", ytdlpBinName);
        } else if(os.contains("mac") || os.contains("darwin")) {
            bin = String.format("mac/%s", ytdlpBinName);
        } else {
            bin = String.format("linux/%s", ytdlpBinName);
        }

        ytdlpBinPath = ytdlpBinPath.resolve(bin).normalize();
    }
    
}
