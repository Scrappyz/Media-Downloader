package com.scrappyz.ytdlp.download.infrastructure.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resources")
@NoArgsConstructor
@Getter @Setter
public class Resource {
    
    @Id
    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expire_at", nullable = true)
    private Instant expireAt;

    @Column(name = "storage_used", nullable = false)
    private long storageUsed;

    @OneToOne
    @MapsId
    @JoinColumn(name = "request_id")
    private Request request;

}
