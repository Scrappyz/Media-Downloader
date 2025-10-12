package com.scrappyz.ytdlp.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DownloadControllerInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DownloadControllerInterceptor.class);
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
        Exception e) throws Exception {

        // MediaService.cleanDownloads(); // Clean the download folders after download is completed
    }
}
