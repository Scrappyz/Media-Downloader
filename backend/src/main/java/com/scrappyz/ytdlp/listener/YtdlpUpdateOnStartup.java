package com.scrappyz.ytdlp.listener;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Executor;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.config.YtdlpConfig;
import com.scrappyz.ytdlp.utils.ProcessUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class YtdlpUpdateOnStartup implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(YtdlpUpdateOnStartup.class);
    
    private final YtdlpConfig ytdlpConfig;
    private final PathProperties paths;
    private final Executor updateExecutor;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        boolean isEmpty = false;

        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(paths.getDownloadPath())) {
            isEmpty = !dirStream.iterator().hasNext();
        } catch(IOException e) {
            e.printStackTrace();
        }

        if(!isEmpty) {
            log.info("[YtdlpUpdateOnStartup.onApplicationEvent] Emptying download directory contents on startup");
            try {
                FileUtils.deleteDirectory(paths.getDownloadPath().toFile());
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        if(!ytdlpConfig.isAutoUpdate()) {
            return;
        }

        log.info("[YtdlpUpdateOnStartup.onApplicationEvent] Updating yt-dlp");

        updateExecutor.execute(() -> {
            try {
                ProcessUtils.runProcess(Arrays.asList(paths.getYtdlpBin().toString(), "-U"));
                log.info("[YtdlpUpdateOnStartup.onApplicationEvent] Updated yt-dlp successfully");
            } catch(IOException | InterruptedException e) {
                log.info("[YtdlpUpdateOnStartup.onApplicationEvent] Failed to update yt-dlp: " + e.getMessage());
            }
        });
    }
}
