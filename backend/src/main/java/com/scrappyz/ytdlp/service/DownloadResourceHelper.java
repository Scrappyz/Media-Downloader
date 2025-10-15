package com.scrappyz.ytdlp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.scrappyz.ytdlp.config.PathProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadResourceHelper {

    private static final Logger log = LoggerFactory.getLogger(DownloadResourceHelper.class);

    private final PathProperties paths;

    @Value("#{${resource.expiry.time} * ${time.multiplier}}")
    private long resourceExpiryTime;
    
    @Async("resourceExecutor")
    public CompletableFuture<Boolean> cleanup(String resourceName) { // Delete downloaded resource after a certain time. Also cleanup
        try {
            Thread.sleep(resourceExpiryTime);
        } catch(InterruptedException e) {
            e.printStackTrace();
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
