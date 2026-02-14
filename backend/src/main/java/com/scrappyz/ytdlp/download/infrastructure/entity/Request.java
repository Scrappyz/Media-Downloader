package com.scrappyz.ytdlp.download.infrastructure.entity;

import java.time.Instant;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "requests")
@NoArgsConstructor
@Getter @Setter
public class Request {
    
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at", nullable = true)
    private Instant completedAt;

    @OneToOne(
        mappedBy = "request",
        orphanRemoval = true,
        cascade = CascadeType.ALL,
        fetch = FetchType.EAGER
    )
    private RequestDetail requestDetail;

}
