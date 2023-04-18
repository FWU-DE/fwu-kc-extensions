package de.intension.api.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class LaufzeitTest
{

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void prepare()
    {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldSerializeLernPeriodeCorrectly()
        throws JsonProcessingException
    {

        Laufzeit laufzeit = new Laufzeit();
        laufzeit.setVonLernPeriode(new Lernperiode("2023/1"));
        laufzeit.setBisLernPeriode(new Lernperiode("2023/2"));

        assertEquals("{\"vonlernperiode\":\"2023/1\",\"bislernperiode\":\"2023/2\"}", objectMapper.writeValueAsString(laufzeit));
    }

    @Test
    void shouldSerializeVonBisCorrectly()
        throws JsonProcessingException
    {
        Laufzeit laufzeit = new Laufzeit();
        laufzeit.setVon(LocalDate.of(2023, 3, 31));
        laufzeit.setBis(LocalDate.of(2023, 3, 31));

        assertEquals("{\"von\":\"2023-03-31\",\"bis\":\"2023-03-31\"}", objectMapper.writeValueAsString(laufzeit));
    }

}