package com.scrappyz.ytdlp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.helper.YtdlpDownloadProcess;
import com.scrappyz.ytdlp.helper.YtdlpProcessResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class YtdlpDownloadProcessHandler implements DownloadProcessHandler<YtdlpProcessResult> {

    private final ConcurrentHashMap<String, YtdlpDownloadProcess> processes = new ConcurrentHashMap<>();
    
    @Override
    public YtdlpProcessResult runProcess(List<String> commands, String id, SseEmitter emitter, ProcessLineHandler processLineHandler, ErrorLineHandler errorLineHandler) throws DownloadFailedException {
        YtdlpProcessResult processResult = new YtdlpProcessResult();
        
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);

            Process process = pb.start();

            processes.put(id, new YtdlpDownloadProcess());
            
            YtdlpDownloadProcess downloadProcess = processes.get(id);
            downloadProcess.setProcess(process);
            downloadProcess.setExecutorService(Executors.newFixedThreadPool(2));

            downloadProcess.getExecutorService().execute(() -> {
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

            downloadProcess.getExecutorService().execute(() -> {
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

            processes.remove(id);
        } catch(IOException | InterruptedException e) {
            processes.remove(id);
            throw new DownloadFailedException();
        }

        return processResult;
    }

    public Process getProcessById(String id) {
        return processes.get(id);
    }

    public void stopProcessById(String id, boolean force) {
        YtdlpDownloadProcess process = processes.get(id);
        if(process != null) {
            process.getExecutorService().shutdownNow();
            if(force) {
                process.getProcess().destroyForcibly();
            } else {
                process.getProcess().destroy();
            }
        }
    }

    public void stopProcessById(String id) {
        stopProcessById(id, true);
    }
    
}
