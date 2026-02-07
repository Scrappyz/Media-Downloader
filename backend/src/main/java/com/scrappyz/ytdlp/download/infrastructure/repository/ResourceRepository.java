package com.scrappyz.ytdlp.download.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.scrappyz.ytdlp.download.infrastructure.entity.Resource;

public interface ResourceRepository extends JpaRepository<Resource, String> {

    Optional<Resource> findByRequestId(String requestId);
    
    @Query("SELECT r FROM Resource r WHERE r.expireAt < CURRENT_TIMESTAMP")
    List<Resource> findExpiredResources();

    @Query("SELECT r FROM Resource r WHERE r.expireAt > CURRENT_TIMESTAMP")
    List<Resource> findNonExpiredResources();

}
