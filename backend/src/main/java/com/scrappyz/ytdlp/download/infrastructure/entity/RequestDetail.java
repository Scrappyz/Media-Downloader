package com.scrappyz.ytdlp.download.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "request_details")
@NoArgsConstructor
@Getter @Setter
public class RequestDetail {
    
    @Id
    @Column(name = "request_id", nullable = false)
    private String requestId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column(name = "video_quality", nullable = true)
    private String videoQuality;

    @Column(name = "video_format", nullable = true)
    private String videoFormat;

    @Column(name = "audio_quality", nullable = true)
    private String audioQuality;

    @Column(name = "audio_format", nullable = true)
    private String audioFormat;

    @Column(name = "metadata", nullable = false)
    private boolean metadata;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToOne
    @JoinColumn(name = "request_id")
    private Request request;
    
}
