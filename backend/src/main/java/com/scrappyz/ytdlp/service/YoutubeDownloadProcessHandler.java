package com.scrappyz.ytdlp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.helper.YoutubeProcessResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class YoutubeDownloadProcessHandler implements DownloadProcessHandler<YoutubeProcessResult> {
    
    @Override
    public YoutubeProcessResult runProcess(List<String> commands, SseEmitter emitter, ProcessLineHandler processLineHandler, ErrorLineHandler errorLineHandler) throws DownloadFailedException {
        YoutubeProcessResult processResult = new YoutubeProcessResult();
        
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);

            Process process = pb.start();

            Thread outputStreamConsumer = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if(!processResult.hasOutputName() && line.startsWith("[download] Destination:")) {
                            int startIndex = line.lastIndexOf('\\');

                            if(startIndex < 0) {
                                startIndex = line.lastIndexOf('/');
                            }

                            String filename = line.substring(startIndex + 1);
                            processResult.setOutputName(filename);
                            continue;
                        }

                        try {
                            processLineHandler.handle(line, emitter); // Pass null or appropriate SseEmitter
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            outputStreamConsumer.start();

            Thread errorStreamConsumer = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try {
                            processResult.setError(errorLineHandler.handle(line)); // Pass null or appropriate SseEmitter
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            errorStreamConsumer.start();

            int exitCode = process.waitFor();

            outputStreamConsumer.join();
            errorStreamConsumer.join();
        } catch(IOException | InterruptedException e) {
            throw new DownloadFailedException();
        }

        return processResult;
    }
    
}
