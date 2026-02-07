package com.scrappyz.ytdlp.download.infrastructure.converter;

import com.scrappyz.ytdlp.download.infrastructure.model.RequestStatus;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RequestStatusConverter implements AttributeConverter<RequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(RequestStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public RequestStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RequestStatus.fromValue(dbData);
    }
    
}
