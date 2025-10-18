package com.scrappyz.ytdlp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all paths under
                .allowedOriginPatterns("http://*:5000", "https://downloader.micoapp.org") // Specific origins
                // .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE") // Allowed HTTP methods
                .allowedHeaders("*") // Allowed headers
                .allowCredentials(true) // Allow sending cookies/authentication headers
                .exposedHeaders("Content-Disposition")
                .maxAge(3600); // Cache preflight response for 1 hour
    }
}