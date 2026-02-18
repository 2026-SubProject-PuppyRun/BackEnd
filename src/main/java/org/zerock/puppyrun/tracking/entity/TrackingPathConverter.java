package org.zerock.puppyrun.tracking.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;
import org.zerock.puppyrun.common.exception.DataIntegrityException;

@Converter
public class TrackingPathConverter implements AttributeConverter<List<TrackingPath>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<TrackingPath> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new DataIntegrityException("경로 데이터를 JSON으로 변환하는데 실패했습니다.", e);
        }
    }

    @Override
    public List<TrackingPath> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new DataIntegrityException("JSON 데이터를 경로 리스트로 변환하는데 실패했습니다.", e);
        }
    }
}
