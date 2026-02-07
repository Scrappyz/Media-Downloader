package com.scrappyz.ytdlp.download.infrastructure.converter;

import com.scrappyz.ytdlp.download.infrastructure.model.VideoFormat;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VideoFormatConverter implements AttributeConverter<VideoFormat, String> {

    @Override
    public String convertToDatabaseColumn(VideoFormat attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public VideoFormat convertToEntityAttribute(String dbData) {
        return dbData == null ? null : VideoFormat.fromValue(dbData);
    }
    
}
