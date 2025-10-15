package com.scrappyz.ytdlp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.DownloadResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadResourceHelper {

    private static final Logger log = LoggerFactory.getLogger(DownloadResourceHelper.class);

    private final PathProperties paths;

    @Value("#{${resource.expiry.time} * ${time.multiplier}}")
    private long resourceExpiryTime;
    
    @Async("resourceExecutor")
    public CompletableFuture<Boolean> cleanup(String id, String resourceName, ConcurrentHashMap<String, CompletableFuture<DownloadResult>> processes, 
        Set<String> cancelled, ConcurrentHashMap<String, String> resourceMap) { // Delete downloaded resource after a certain time. Also cleanup
        
        try {
            Thread.sleep(resourceExpiryTime);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        if(processes.containsKey(id)) {
            log.info("[DownloadResourceHelper.cleanup] Process ID " + id + " expired");
            processes.remove(id);
        }

        if(cancelled.contains(id)) {
            log.info("[DownloadResourceHelper.cleanup] Cancel ID " + id + " expired");
            cancelled.remove(id);
        }

        if(resourceMap.containsKey(id)) {
            log.info("[DownloadResourceHelper.cleanup] Resource ID " + id + " expired");
            resourceMap.remove(id);
        }

        Path resourcePath = paths.getDownloadPath().resolve(resourceName).normalize();

        try {
            boolean deleted = Files.deleteIfExists(resourcePath);
            log.info("Resource \"" + resourceName + "\" expired");
            return CompletableFuture.completedFuture(deleted);
        } catch(IOException e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }
}
