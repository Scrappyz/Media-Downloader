package com.scrappyz.ytdlp.startup;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.scrappyz.ytdlp.config.properties.PathProperties;
import com.scrappyz.ytdlp.config.properties.YtdlpProperties;
import com.scrappyz.ytdlp.download.domain.service.DownloadRepositoryService;
import com.scrappyz.ytdlp.download.domain.service.helper.DownloadResourceHelper;
import com.scrappyz.ytdlp.download.infrastructure.entity.Resource;
import com.scrappyz.ytdlp.download.infrastructure.repository.ResourceRepository;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Run startup stuff here
@Component
@RequiredArgsConstructor
@Slf4j
public class YtdlpStartupListener implements ApplicationListener<ApplicationReadyEvent> {
    
    private final YtdlpProperties ytdlpProperties;
    private final PathProperties paths;
    private final DownloadResourceHelper resourceHelper;
    private final ExecutorService startupExecutor;
    private final ResourceRepository resourceRepository;
    private final DownloadRepositoryService downloadRepositoryService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {

        // Check if yt-dlp binary exists
        if(!hasYtdlpBinary()) {
            log.info("[YtdlpStartupListener.onApplicationEvent] yt-dlp binary not found, aborting execution");
            throw new RuntimeException("yt-dlp binary not found at path: " + paths.getYtdlpBinPath().toString());
        }

        // Auto-update yt-dlp if enabled
        if(ytdlpProperties.isAutoUpdate()) {
            startupExecutor.execute(() -> {
                List<String> commands = Arrays.asList(paths.getYtdlpBinPath().toString(), "-U");
                try {
                    ProcessBuilder pb = new ProcessBuilder(commands);

                    Process process = pb.start();

                    int exitCode = process.waitFor();

                    log.info("[YtdlpStartupListener.onApplicationEvent] Updated yt-dlp successfully");
                } catch(IOException | InterruptedException e) {
                    log.info("[YtdlpStartupListener.onApplicationEvent] Failed to update yt-dlp: " + e.getMessage());
                }
            });

            log.info("[YtdlpStartupListener.onApplicationEvent] Updating yt-dlp");
        }

        // Check if download path exists, if not create it
        if(paths.getDownloadPath().toFile().exists()) {
            log.info("[YtdlpStartupListener.onApplicationEvent] Download path exists: " + paths.getDownloadPath().toString());
        } else {
            log.info("[YtdlpStartupListener.onApplicationEvent] Download path does not exist, creating: " + paths.getDownloadPath().toString());
            try {
                paths.getDownloadPath().toFile().mkdirs();
                log.info("[YtdlpStartupListener.onApplicationEvent] Created download path successfully");
            } catch(Exception e) {
                log.info("[YtdlpStartupListener.onApplicationEvent] Failed to create download path: " + e.getMessage());
                throw new RuntimeException("Failed to create download path: " + e.getMessage());
            }
        }

        // Check for expired resources since the application was last run and remove them
        List<Resource> resources = resourceRepository.findAllNonDeletedResources();
        List<String> deletedResourceIds = new ArrayList<>();
        for(int i = 0; i < resources.size(); i++) {
            String id = resources.get(i).getRequestId();
            Instant expireAt = resources.get(i).getExpireAt();
            Instant deletedAt = resources.get(i).getDeletedAt();
            boolean isExpired = expireAt.isBefore(Instant.now());
            boolean isDeleted = deletedAt != null;

            if(isExpired && !isDeleted) {
                log.info("[YtdlpStartupListener.onApplicationEvent] Removing resource: " + id);
                resourceHelper.removeResource(id);
                deletedResourceIds.add(id);
            }

            if(!isExpired && !isDeleted) {
                log.info("[YtdlpStartupListener.onApplicationEvent] Queuing resource for expiry: " + id);
                resourceHelper.queue(id, expireAt, resources.get(i).getStorageUsed());
            }
        }

        if(!deletedResourceIds.isEmpty()) {
            downloadRepositoryService.updateDeletedAtForResources(deletedResourceIds);
        }

        startupExecutor.execute(() -> resourceHelper.run()); // Expire resources
    }

    @PreDestroy
    private void stopExecutor() {
        startupExecutor.shutdownNow();
    }

    private boolean hasYtdlpBinary() {
        return paths.getYtdlpBinPath().toFile().exists();
    }
}
