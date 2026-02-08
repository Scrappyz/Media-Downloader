package com.scrappyz.ytdlp.download.infrastructure.converter;

import com.scrappyz.ytdlp.download.infrastructure.model.AudioFormat;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AudioFormatConverter implements AttributeConverter<AudioFormat, String> {

    @Override
    public String convertToDatabaseColumn(AudioFormat attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public AudioFormat convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AudioFormat.fromValue(dbData);
    }
    
}
