package com.scrappyz.ytdlp.download.infrastructure.converter;

import com.scrappyz.ytdlp.download.infrastructure.model.RequestType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RequestTypeConverter implements AttributeConverter<RequestType, String> {
    
    @Override
    public String convertToDatabaseColumn(RequestType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public RequestType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RequestType.fromValue(dbData);
    }
}
