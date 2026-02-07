package com.scrappyz.ytdlp.download.domain.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.DelayQueue;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.download.domain.exception.custom.ResourceNotFoundException;
import com.scrappyz.ytdlp.download.domain.model.ExpiringResource;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadResourceHelper {

    private static final Logger log = LoggerFactory.getLogger(DownloadResourceHelper.class);

    private final PathProperties paths;

    @Value("${resource.expiry.time}")
    private Duration resourceExpiryTime;

    private long occupiedStorage;

    private final DelayQueue<ExpiringResource> queue = new DelayQueue<>();

    // Run the resource helper for throughout the whole runtime
    public void run() {
        log.info("[DownloadResourceHelper.run] Running resource manager");
        while(true) {
            try {
                ExpiringResource resource = queue.take();
                String id = resource.getId();
                Path resourcePath = paths.getDownloadPath().resolve(id).normalize();
                long fileSize = resource.getFileSize();

                log.info("[DownloadResourceHelper.run] '" + id + "' has expired");
                FileUtils.deleteDirectory(new File(resourcePath.toString()));
            
                occupiedStorage -= fileSize;
                log.info("[DownloadResourceHelper.run] Occupied Storage: " + occupiedStorage);
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

        long fileSize = getFileSize(id);

        queue.put(new ExpiringResource(id, expiryMillis, fileSize));

        occupiedStorage += fileSize;
        log.info("[DownloadResourceHelper.queue] Occupied Storage: " + occupiedStorage);
    }

    public File getFile(String id) {
        Path resourcePath = paths.getDownloadPath().resolve(id).normalize();

        File directory = new File(resourcePath.toString());
        if(!directory.exists() || !directory.isDirectory()) {
            throw new ResourceNotFoundException("The resource could not be found or has expired");
        }

        File[] files = directory.listFiles();
        if(files == null || files.length == 0) {
            throw new ResourceNotFoundException("The resource could not be found or has expired");
        }

        return files[0];
    }

    public long getFileSize(String id) {
        File file = getFile(id);
        long fileSize = 0;

        try {
            fileSize = Files.size(file.toPath());
        } catch (IOException e) {
            log.info("[DownloadResourceHelper.getFileSize] IOException");
        }

        return fileSize;
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
