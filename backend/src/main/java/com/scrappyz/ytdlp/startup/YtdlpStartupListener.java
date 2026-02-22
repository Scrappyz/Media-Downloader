package com.scrappyz.ytdlp.startup;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

// Run startup stuff here
@Component
@RequiredArgsConstructor
public class YtdlpStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(YtdlpStartupListener.class);
    
    private final YtdlpProperties ytdlpProperties;
    private final PathProperties paths;
    private final DownloadResourceHelper resourceHelper;
    private final ExecutorService startupExecutor;
    private final ResourceRepository resourceRepository;
    private final DownloadRepositoryService downloadRepositoryService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        boolean isEmpty = false;

        // try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(paths.getDownloadPath())) {
        //     isEmpty = !dirStream.iterator().hasNext();
        // } catch(IOException e) {
        //     e.printStackTrace();
        // }

        // if(!isEmpty) {
        //     log.info("[YtdlpStartupListener.onApplicationEvent] Emptying download directory contents on startup");
        //     try {
        //         FileUtils.cleanDirectory(paths.getDownloadPath().toFile());
        //     } catch(IOException e) {
        //         e.printStackTrace();
        //     }
        // }

        if(ytdlpProperties.isAutoUpdate()) {
            startupExecutor.execute(() -> {
                List<String> commands = Arrays.asList(paths.getYtdlpBin().toString(), "-U");
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

        List<Resource> deleteResources = resourceRepository.findAllNonDeletedExpiredResources();
        for(int i = 0; i < deleteResources.size(); i++) {
            String id = deleteResources.get(i).getRequestId();
            log.info("[YtdlpStartupListener.onApplicationEvent] Removing resource: " + id);
            resourceHelper.removeResource(id);
            downloadRepositoryService.updateDeletedAtForResource(id);
        }

        startupExecutor.execute(() -> resourceHelper.run()); // Expire resources
    }

    @PreDestroy
    private void stopExecutor() {
        startupExecutor.shutdownNow();
    }
}
