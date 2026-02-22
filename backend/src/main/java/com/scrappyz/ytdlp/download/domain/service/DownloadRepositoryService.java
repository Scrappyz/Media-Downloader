package com.scrappyz.ytdlp.download.domain.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.scrappyz.ytdlp.download.infrastructure.entity.Request;
import com.scrappyz.ytdlp.download.infrastructure.entity.Resource;
import com.scrappyz.ytdlp.download.infrastructure.repository.RequestRepository;
import com.scrappyz.ytdlp.download.infrastructure.repository.ResourceRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DownloadRepositoryService {

    private final RequestRepository requestRepository;
    private final ResourceRepository resourceRepository;

    @Transactional
    public void addNewRequest(Request request) {
        requestRepository.save(request);
    }

    @Transactional
    public void updateRequestStatusById(String requestId, String status) {
        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));
        request.setStatus(status);
    }

    @Transactional
    public void completeRequestById(String requestId) {
        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));
        request.setStatus("completed");
        request.setCompletedAt(Instant.now());
    }

    @Transactional
    public void addNewResource(String requestId, Instant createdAt, Instant expireAt, long storageUsed) {
        Request request = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));

        Resource resource = new Resource();
        resource.setRequestId(requestId);
        resource.setCreatedAt(createdAt);
        resource.setExpireAt(expireAt);
        resource.setStorageUsed(storageUsed);
        resource.setRequest(request);

        resourceRepository.save(resource);
    }

    @Transactional
    public void updateDeletedAtForResource(String requestId) {
        Resource resource = resourceRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Resource not found with request ID: " + requestId));
        resource.setDeletedAt(Instant.now());
    }

    @Transactional
    public void incrementFetchCountByRequestId(String requestId) {
        Resource resource = resourceRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));
        resource.setFetchCount(resource.getFetchCount() + 1);
    }

}
