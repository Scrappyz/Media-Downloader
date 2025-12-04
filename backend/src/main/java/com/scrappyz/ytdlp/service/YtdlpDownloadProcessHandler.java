package com.scrappyz.ytdlp.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

            processes.put(id, new YtdlpDownloadProcess(process, Executors.newFixedThreadPool(2)));

            YtdlpDownloadProcess downloadProcess = processes.get(id);

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

            int exitCode = process.waitFor();

            if(exitCode != 0) {
                throw new DownloadFailedException();
            }

            processes.remove(id);
        } catch(IOException | InterruptedException e) {
            processes.remove(id);
            throw new DownloadFailedException();
        }

        return processResult;
    }

    public YtdlpDownloadProcess getProcessById(String id) {
        return processes.get(id);
    }

    public void stopProcessById(String id, boolean force) {
        YtdlpDownloadProcess downloadProcess = processes.get(id);
        if (downloadProcess == null) return;

        Process p = downloadProcess.getProcess();
        var exec = downloadProcess.getExecutorService();

        // Close streams
        try { 
            if(p != null) p.getInputStream().close(); 
        } catch (IOException ignore) {}

        try { 
            if(p != null) p.getErrorStream().close(); 
        } catch (IOException ignore) {}

        try { 
            if(p != null) p.getOutputStream().close(); 
        } catch (IOException ignore) {}

        // Kill subprocesses
        if(p != null) {
            try {
                p.toHandle().descendants().forEach(ph -> {
                    try { 
                        ph.destroyForcibly(); 
                    } catch (Exception ignore) {}
                });
            } catch (UnsupportedOperationException ignored) {}
        }

        // Kill parent
        if(p != null) {
            if(force) p.destroyForcibly();
            else p.destroy();
        }

        // Stop executor
        if(exec != null) {
            exec.shutdownNow();
            try { 
                exec.awaitTermination(2, TimeUnit.SECONDS); 
            } catch (InterruptedException e) { 
                Thread.currentThread().interrupt(); 
            }
        }

        processes.remove(id);
    }

    public void stopProcessById(String id) {
        stopProcessById(id, true);
    }

    public boolean hasProcess(String id) {
        return processes.containsKey(id);
    }
    
}
// Shoulder: 19 inches
// Length: 24 inches
// Arm: 13 inches
// Waist: 38 inches