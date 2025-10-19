package com.scrappyz.ytdlp.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
@ConfigurationPropertiesScan("com.scrappyz.ytdlp.config")
public class AppConfig {
    
}
