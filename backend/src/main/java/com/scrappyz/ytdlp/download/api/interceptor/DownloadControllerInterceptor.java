package com.scrappyz.ytdlp.download.api.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DownloadControllerInterceptor implements HandlerInterceptor {

    // Removed unused logger variable
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
        Exception e) throws Exception {

        // MediaService.cleanDownloads(); // Clean the download folders after download is completed
    }
}
