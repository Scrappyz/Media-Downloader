package com.scrappyz.ytdlp.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "path")
@Validated
@Getter @Setter
public class PathProperties {
    
    private Path executablePath;
    private Path downloadPath;
    
}
