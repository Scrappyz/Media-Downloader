package com.scrappyz.ytdlp.download.domain.service;

import org.springframework.core.io.FileSystemResource;

import com.scrappyz.ytdlp.download.api.dto.DownloadRequest;
import com.scrappyz.ytdlp.download.api.dto.DownloadResponse;

public interface DownloadService {

    public DownloadResponse enqueue(DownloadRequest request);

    public FileSystemResource getResource(String id);

    public void cancelDownload(String id);

    public DownloadRequest getDownloadRequest(String id);

}
