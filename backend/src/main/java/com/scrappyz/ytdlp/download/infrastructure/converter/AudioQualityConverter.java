package com.scrappyz.ytdlp.download.infrastructure.converter;

import com.scrappyz.ytdlp.download.infrastructure.model.AudioQuality;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AudioQualityConverter implements AttributeConverter<AudioQuality, String> {

    @Override
    public String convertToDatabaseColumn(AudioQuality attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AudioQuality convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AudioQuality.fromValue(dbData);
    }
    
}
