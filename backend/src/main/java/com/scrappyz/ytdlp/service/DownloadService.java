package com.scrappyz.ytdlp.service;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;
import com.scrappyz.ytdlp.dto.DownloadResult;
import com.scrappyz.ytdlp.exception.custom.FullDownloadQueueException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    private final PathProperties paths;

    private final DownloadHelper downloadHelper;

    public enum RequestStatus {
        SUCCESS("success"),
        FAILED("failed"),
        PROCESSING("processing"),
        PENDING("pending"),
        INVALID("invalid");

        private final String string;
        private static final HashMap<String, RequestStatus> byString = new HashMap<>();

        static {
            for(RequestStatus t: values()) {
                byString.put(t.string, t);
            }
        }

        private RequestStatus(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public static RequestStatus getRequestStatus(String str) {
            return byString.get(str);
        }
    };

    // Queue the download request
    public DownloadResponse enqueue(DownloadRequest request) {
        DownloadResponse result = new DownloadResponse();
        CompletableFuture<DownloadResult> f = new CompletableFuture<>();
        String id = UlidCreator.getMonotonicUlid().toString();

        try {
            f = downloadHelper.download(id, request); // Run in the background
        } catch(RejectedExecutionException e) {
            log.info("[ERROR] Rejected Execution due to full queue");
            throw new FullDownloadQueueException("Download queue is full");
        }

        result.setRequestId(id);

        downloadHelper.addProcess(id, f);

        return result;
    }

    public CompletableFuture<DownloadResult> getProcess(String id) {
        return downloadHelper.getProcess(id);
    }

    public boolean removeProcess(String id) {
        return downloadHelper.removeProcess(id);
    }

    public boolean cancelProcess(String id) {
        return downloadHelper.cancelProcess(id);
    }

    public FileSystemResource getResource(String id) {
        return downloadHelper.getResource(id, true);
    }

    public FileSystemResource getResource(String id, boolean removeInResourceMap) {
        return downloadHelper.getResource(id, removeInResourceMap);
    }

    public boolean isProcessExist(String id) {
        return downloadHelper.isProcessExist(id);
    }
    
}
