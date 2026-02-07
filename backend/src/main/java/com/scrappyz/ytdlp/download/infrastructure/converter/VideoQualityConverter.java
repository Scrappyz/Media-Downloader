package com.scrappyz.ytdlp.download.infrastructure.converter;

import com.scrappyz.ytdlp.download.infrastructure.model.VideoQuality;

import jakarta.persistence.AttributeConverter;

public class VideoQualityConverter implements AttributeConverter<VideoQuality, String> {

    @Override
    public String convertToDatabaseColumn(VideoQuality attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public VideoQuality convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VideoQuality.fromValue(dbData);
    }
    
}
