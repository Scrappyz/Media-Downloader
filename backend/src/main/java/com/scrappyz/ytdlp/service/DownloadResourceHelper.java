package com.scrappyz.ytdlp.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.DelayQueue;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.helper.ExpiringResource;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadResourceHelper {

    private static final Logger log = LoggerFactory.getLogger(DownloadResourceHelper.class);

    private final PathProperties paths;

    @Value("${resource.expiry.time}")
    private Duration resourceExpiryTime;

    private final DelayQueue<ExpiringResource> queue = new DelayQueue<>();

    // Run the resource helper for throughout the whole runtime
    public void run() {
        while(true) {
            try {
                ExpiringResource resource = queue.take();
                String id = resource.getId();
                Path resourcePath = paths.getDownloadPath().resolve(id).normalize();
                
                log.info("[DownloadResourceHelper.run] '" + id + "' has expired");
                FileUtils.deleteDirectory(new File(resourcePath.toString()));
            
            } catch(IOException e) {
                e.printStackTrace();
            } catch(InterruptedException e) {
                log.info("[DownloadResourceHelper.run] Resource Manager has been stopped");
            }
        }
    }

    // Add an item in the queue for expiry
    public void queue(String id) {
        long expiryMillis = resourceExpiryTime.toMillis();
        log.info("[DownloadResourceHelper.queue] '" + id + "' has been queued for expiry in " + expiryMillis + " ms");
        queue.put(new ExpiringResource(id, expiryMillis));
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
