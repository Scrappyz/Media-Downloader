package com.scrappyz.ytdlp.download.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.scrappyz.ytdlp.download.infrastructure.entity.Resource;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, String> {
    
    @Query("SELECT r FROM Resource r WHERE r.expireAt <= CURRENT_TIMESTAMP")
    List<Resource> findAllExpiredResources();

    @Query("SELECT r FROM Resource r WHERE r.expireAt > CURRENT_TIMESTAMP")
    List<Resource> findAllNonExpiredResources();

    @Query("SELECT r FROM Resource r WHERE r.expireAt <= CURRENT_TIMESTAMP AND r.deletedAt IS NULL")
    List<Resource> findAllNonDeletedExpiredResources();
}
