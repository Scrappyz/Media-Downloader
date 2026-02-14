package com.scrappyz.ytdlp.download.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scrappyz.ytdlp.download.infrastructure.entity.Request;

public interface RequestRepository extends JpaRepository<Request, String> {

}
