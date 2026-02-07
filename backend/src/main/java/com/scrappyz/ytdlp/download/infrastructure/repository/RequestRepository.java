package com.scrappyz.ytdlp.download.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.scrappyz.ytdlp.download.infrastructure.entity.Request;

import org.springframework.transaction.annotation.Transactional;

public interface RequestRepository extends JpaRepository<Request, String> {

    @Override
    Optional<Request> findById(String id);
    
    @Modifying
    @Transactional
    @Query("UPDATE Request r SET r.status = :status WHERE r.id = :requestId")
    void updateStatusById(String requestId, String status);

    @Modifying
    @Transactional
    @Query("UPDATE Request r SET r.completedAt = CURRENT_TIMESTAMP WHERE r.id = :requestId")
    void updateCompletedAtById(String requestId);

}
