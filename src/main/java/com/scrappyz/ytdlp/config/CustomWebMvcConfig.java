package com.scrappyz.ytdlp.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import com.scrappyz.ytdlp.interceptor.MediaControllerInterceptor;

@Configuration
public class CustomWebMvcConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MediaControllerInterceptor())
                .addPathPatterns("/download");
    }

    // For static resources
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(System.getProperty("user.dir")).resolve("storage/public").normalize();

        registry.addResourceHandler("/public/**") // Prefix; Becomes `{context-path}/public/**` to access the `temp` folder
                .addResourceLocations("file:" + path.toString() + "/") // `file:` means that the directory is within the filesystem
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .resourceChain(true)
                .addResolver(new PathResourceResolver()); // guards against ../ traversal
    }
}
