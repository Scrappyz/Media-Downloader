package com.scrappyz.ytdlp.download.infrastructure.entity;

import com.scrappyz.ytdlp.download.infrastructure.converter.AudioFormatConverter;
import com.scrappyz.ytdlp.download.infrastructure.converter.AudioQualityConverter;
import com.scrappyz.ytdlp.download.infrastructure.converter.RequestTypeConverter;
import com.scrappyz.ytdlp.download.infrastructure.converter.VideoFormatConverter;
import com.scrappyz.ytdlp.download.infrastructure.converter.VideoQualityConverter;
import com.scrappyz.ytdlp.download.infrastructure.model.AudioFormat;
import com.scrappyz.ytdlp.download.infrastructure.model.AudioQuality;
import com.scrappyz.ytdlp.download.infrastructure.model.RequestType;
import com.scrappyz.ytdlp.download.infrastructure.model.VideoFormat;
import com.scrappyz.ytdlp.download.infrastructure.model.VideoQuality;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
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
    @Convert(converter = RequestTypeConverter.class)
    private RequestType requestType;

    @Column(name = "video_quality", nullable = true)
    @Convert(converter = VideoQualityConverter.class)
    private VideoQuality videoQuality;

    @Column(name = "video_format", nullable = true)
    @Convert(converter = VideoFormatConverter.class)
    private VideoFormat videoFormat;

    @Column(name = "audio_quality", nullable = true)
    @Convert(converter = AudioQualityConverter.class)
    private AudioQuality audioQuality;

    @Column(name = "audio_format", nullable = true)
    @Convert(converter = AudioFormatConverter.class)
    private AudioFormat audioFormat;

    @Column(name = "metadata", nullable = false)
    private boolean metadata;

    // Add this to RequestDetail.java
    @Version
    @Column(name = "version")
    private Long version;

    @OneToOne
    @MapsId
    @JoinColumn(name = "request_id")
    private Request request;
    
}
