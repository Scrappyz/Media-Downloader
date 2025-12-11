package com.scrappyz.ytdlp.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import org.apache.commons.io.FileUtils;
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

    @Value("${resource.expiry.time}")
    private Duration resourceExpiryTime;
    
    @Async("resourceExecutor")
    public void cleanup(String id) { // Delete downloaded resource after a certain time. Also cleanup

        long expiryMillis = resourceExpiryTime.toMillis();

        Path resourcePath = paths.getDownloadPath().resolve(id).normalize();

        try {
            log.info("[DownloadResourceHelper.cleanup] Waiting " + expiryMillis + "ms before deleting resource '" + id + "'");
            Thread.sleep(expiryMillis);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        try {
            log.info("[DownloadResourceHelper.cleanup] '" + id + "' has expired");
            FileUtils.deleteDirectory(new File(resourcePath.toString()));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public boolean removeResource(String id) {
        Path resourcePath = paths.getDownloadPath().resolve(id).normalize();

        try {
            log.info("[DownloadResourceHelper.removeResource] Deleting resource '" + id + "'");
            FileUtils.deleteDirectory(new File(resourcePath.toString()));
            return true;
        } catch(IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
