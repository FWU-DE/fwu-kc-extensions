package de.intension.api.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class LoeschungTest
{

    @Test
    void shouldSerializeToJson()
        throws JsonProcessingException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Loeschung loeschung = new Loeschung(LocalDateTime.of(2099, 12, 31, 23, 59));
        assertEquals("{\"zeitpunkt\":\"2099-12-31T23:59Z\"}", objectMapper.writeValueAsString(loeschung));
    }

}