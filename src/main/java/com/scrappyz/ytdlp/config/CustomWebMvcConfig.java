package com.scrappyz.ytdlp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.scrappyz.ytdlp.interceptor.MediaControllerInterceptor;

@Configuration
public class CustomWebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MediaControllerInterceptor())
                .addPathPatterns("/download");
    }
}
