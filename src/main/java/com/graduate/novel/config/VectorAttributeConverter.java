package com.graduate.novel.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Converter for PostgreSQL vector type
 * Converts float[] to String format: "[1.0,2.0,3.0]"
 */
@Converter(autoApply = true)
public class VectorAttributeConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }

        // Convert float[] to PostgreSQL vector format: [1.0,2.0,3.0]
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < attribute.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(attribute[i]);
        }
        sb.append("]");

        return sb.toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }

        // Remove brackets and split by comma
        String cleanData = dbData.replaceAll("[\\[\\]]", "");
        if (cleanData.isEmpty()) {
            return new float[0];
        }

        String[] values = cleanData.split(",");
        float[] result = new float[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = Float.parseFloat(values[i].trim());
        }

        return result;
    }
}

