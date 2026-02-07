package com.scrappyz.ytdlp.download.domain.service.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.download.domain.exception.custom.DownloadFailedException;
import com.scrappyz.ytdlp.download.domain.model.YtdlpDownloadProcess;
import com.scrappyz.ytdlp.download.domain.model.YtdlpProcessResult;
import com.scrappyz.ytdlp.download.domain.service.impl.YtdlpDownloadService.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class YtdlpDownloadProcessHandler implements DownloadProcessHandler<YtdlpProcessResult> {

    private static final Logger log = LoggerFactory.getLogger(YtdlpDownloadProcessHandler.class);

    private final ConcurrentHashMap<String, YtdlpDownloadProcess> processes = new ConcurrentHashMap<>();
    
    @Override
    public YtdlpProcessResult runProcess(List<String> commands, String id, SseEmitter emitter, ProcessLineHandler processLineHandler, ErrorLineHandler errorLineHandler) throws DownloadFailedException {
        YtdlpProcessResult processResult = new YtdlpProcessResult();
        YtdlpDownloadProcess downloadProcess = new YtdlpDownloadProcess();
        
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);

            Process process = pb.start();

            processes.put(id, new YtdlpDownloadProcess(process, Executors.newFixedThreadPool(2)));

            downloadProcess = processes.get(id);
            downloadProcess.setRunning(true);

            downloadProcess.getExecutorService().execute(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        ErrorCode error = ErrorCode.NONE; // Placeholder for error handling logic
                        boolean readable = processes.get(id).isReadable();
                        try {
                            processLineHandler.handle(line, emitter);
                            if(readable) processResult.setError(error); // Pass null or appropriate SseEmitter
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if(processResult.getError() != ErrorCode.NONE) {
                            processes.get(id).setReadable(false);
                            stopProcessById(id);
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
                        ErrorCode error = ErrorCode.NONE;
                        boolean readable = processes.get(id).isReadable();
                        try {
                            errorLineHandler.handle(line);
                            if(readable) processResult.setError(error); // Pass null or appropriate SseEmitter
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            process.waitFor(); // Removed unused variable 'exitCode'
        } catch(IOException | InterruptedException e) {
            log.info("[YtdlpDownloadProcessHandler.runProcess] Download got interrupted");
            downloadProcess.setRunning(false);
            processes.remove(id);
            throw new DownloadFailedException();
        }

        downloadProcess.setRunning(false);
        return processResult;
    }

    public YtdlpDownloadProcess getProcessById(String id) {
        return processes.get(id);
    }

    public void stopProcess(YtdlpDownloadProcess downloadProcess, boolean cancel, boolean force) {
        if(downloadProcess == null) {
            log.info("[YtdlpDownloadProcessHandler.stopProcessById] Process does not exist");
            return;
        }

        if(!downloadProcess.isRunning()) {
            log.info("[YtdlpDownloadProcessHandler.stopProcessById] Process is no longer running");
            return;
        }

        Process p = downloadProcess.getProcess();
        var exec = downloadProcess.getExecutorService();

        // Close streams
        try { 
            if(p != null) p.getInputStream().close(); 
        } catch(IOException ignore) {
            log.info("[YtdlpDownloadProcessHandler.stopProcessById] Input Stream Closed");
        }

        try { 
            if(p != null) p.getErrorStream().close(); 
        } catch(IOException ignore) {
            log.info("[YtdlpDownloadProcessHandler.stopProcessById] Error Stream Closed");
        }

        try { 
            if(p != null) p.getOutputStream().close(); 
        } catch(IOException ignore) {
            log.info("[YtdlpDownloadProcessHandler.stopProcessById] Output Stream Closed");
        }

        // Kill subprocesses
        if(p != null) {
            try {
                p.toHandle().descendants().forEach(ph -> {
                    try { 
                        ph.destroyForcibly(); 
                    } catch (Exception ignore) {}
                });
            } catch(UnsupportedOperationException ignored) {}
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

        downloadProcess.setCancelled(cancel);
        downloadProcess.setRunning(false);
    }

    public void cancelProcessById(String id) {
        log.info("[YtdlpDownloadProcessHandler.stopProcessById] Process '" + id + "' is being stopped");
        YtdlpDownloadProcess downloadProcess = processes.get(id);

        stopProcess(downloadProcess, true, true);
    }

    public void stopProcessById(String id) {
        log.info("[YtdlpDownloadProcessHandler.stopProcessById] Process '" + id + "' is being stopped");
        YtdlpDownloadProcess downloadProcess = processes.get(id);

        stopProcess(downloadProcess, false, true);
    }

    public boolean hasProcess(String id) {
        return processes.containsKey(id);
    }

    public boolean isProcessRunning(String id) {
        YtdlpDownloadProcess downloadProcess = processes.get(id);
        if(downloadProcess == null) return false;

        return downloadProcess.isRunning();
    }

    public boolean isProcessCancelled(String id) {
        YtdlpDownloadProcess downloadProcess = processes.get(id);
        if(downloadProcess == null) return false;

        return downloadProcess.isCancelled();
    }

    public void removeProcessById(String id) {
        processes.remove(id);
    }
    
}