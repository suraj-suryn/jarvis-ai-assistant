package com.jarus.ai.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jarus.ai.model.ResumeSection;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

public class JpaConverters {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Converter(autoApply = false)
    public static class StringListConverter implements AttributeConverter<List<String>, String> {
        @Override
        public String convertToDatabaseColumn(List<String> list) {
            if (list == null || list.isEmpty()) return "[]";
            try {
                return MAPPER.writeValueAsString(list);
            } catch (JsonProcessingException e) {
                return "[]";
            }
        }

        @Override
        public List<String> convertToEntityAttribute(String json) {
            if (json == null || json.isBlank()) return new ArrayList<>();
            try {
                return MAPPER.readValue(json, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
    }

    @Converter(autoApply = false)
    public static class ResumeSectionListConverter implements AttributeConverter<List<ResumeSection>, String> {
        @Override
        public String convertToDatabaseColumn(List<ResumeSection> list) {
            if (list == null || list.isEmpty()) return "[]";
            try {
                return MAPPER.writeValueAsString(list);
            } catch (JsonProcessingException e) {
                return "[]";
            }
        }

        @Override
        public List<ResumeSection> convertToEntityAttribute(String json) {
            if (json == null || json.isBlank()) return new ArrayList<>();
            try {
                return MAPPER.readValue(json, new TypeReference<List<ResumeSection>>() {});
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
    }
}
