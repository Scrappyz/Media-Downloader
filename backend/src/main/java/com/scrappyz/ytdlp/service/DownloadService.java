package com.scrappyz.ytdlp.service;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.github.f4b6a3.ulid.UlidCreator;
import com.scrappyz.ytdlp.config.DownloadProperties;
import com.scrappyz.ytdlp.config.PathProperties;
import com.scrappyz.ytdlp.dto.DownloadRequest;
import com.scrappyz.ytdlp.dto.DownloadResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    private final PathProperties paths;
    private final DownloadProperties downloadProperties;

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
        String id = UlidCreator.getMonotonicUlid().toString();

        downloadHelper.addEmitter(id, new SseEmitter(downloadProperties.getTimeout().toMillis()));
        downloadHelper.download(id, request);

        result.setRequestId(id);

        return result;
    }

    public void cancelDownload(String id) {
        downloadHelper.cancelDownload(id);
    }

    public FileSystemResource getResource(String id) {
        return downloadHelper.getResource(id);
    }

    public SseEmitter getEmitter(String id) {
        return downloadHelper.getEmitter(id);
    }
    
}
