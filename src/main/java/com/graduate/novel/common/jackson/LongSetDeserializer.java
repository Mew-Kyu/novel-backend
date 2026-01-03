package com.graduate.novel.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom deserializer for Set<Long> that handles edge cases:
 * - Empty object {} -> empty set
 * - null -> null
 * - array [1,2,3] -> set with values
 */
public class LongSetDeserializer extends JsonDeserializer<Set<Long>> {

    @Override
    public Set<Long> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();

        // Handle null
        if (token == JsonToken.VALUE_NULL) {
            return null;
        }

        // Handle empty object {} - treat as empty set
        if (token == JsonToken.START_OBJECT) {
            // Skip the empty object
            p.nextToken(); // Move to END_OBJECT
            return new HashSet<>();
        }

        // Handle array [1, 2, 3]
        if (token == JsonToken.START_ARRAY) {
            Set<Long> result = new HashSet<>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                result.add(p.getLongValue());
            }
            return result;
        }

        // Unexpected token
        throw new IOException(
            String.format("Expected array or null for genreIds, but got: %s. " +
                         "Please send genreIds as an array (e.g., [1, 2, 3]) or omit the field.",
                         token)
        );
    }
}

