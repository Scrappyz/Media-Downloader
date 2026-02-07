package com.scrappyz.ytdlp.download.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.scrappyz.ytdlp.download.infrastructure.entity.Request;

public interface RequestRepository extends JpaRepository<Request, String> {
    
    @Modifying
    @Query("UPDATE Request r SET r.status = :status WHERE r.id = :requestId")
    void updateStatusById(String requestId, String status);

    @Modifying
    @Query("UPDATE Request r SET r.completedAt = CURRENT_TIMESTAMP WHERE r.id = :requestId")
    void updateCompletedAtById(String requestId);
}
