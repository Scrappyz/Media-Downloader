package com.scrappyz.ytdlp.download.domain.service;

import java.util.HashMap;

import org.springframework.core.io.FileSystemResource;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.scrappyz.ytdlp.download.api.dto.DownloadRequest;
import com.scrappyz.ytdlp.download.api.dto.DownloadResponse;

public interface DownloadService {

    public DownloadResponse enqueue(DownloadRequest request);

    public SseEmitter getEmitter(String id);

    public FileSystemResource getResource(String id);

    public void cancelDownload(String id);

}
