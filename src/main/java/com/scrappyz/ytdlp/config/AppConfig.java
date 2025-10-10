package com.scrappyz.ytdlp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
@EnableConfigurationProperties(PathProperties.class)
public class AppConfig {
    
}
