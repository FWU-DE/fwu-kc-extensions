package de.intension.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utility class to handle simple JSON serializable in Keycloak.
 * Wraps the Jackson {@link ObjectMapper} to provide JSON serialization and deserialization.
 */
public class JsonSerialization {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(new JavaTimeModule());
    }

    public static <T> T readValue(String content, Class<T> valueType) throws JsonProcessingException {
        return mapper.readValue(content, valueType);
    }

    public static String writeValueAsString(Object value) throws JsonProcessingException {
        return mapper.writeValueAsString(value);
    }
}
