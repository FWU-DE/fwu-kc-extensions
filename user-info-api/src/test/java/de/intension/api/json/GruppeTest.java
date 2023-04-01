package de.intension.api.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.intension.api.enumerations.*;

class GruppeTest
{

    @Test
    void shouldSerializeToJson()
        throws JsonProcessingException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        Laufzeit laufzeit = new Laufzeit();
        laufzeit.setVon(LocalDate.now());
        laufzeit.setBis(LocalDate.now());
        Gruppe gruppe = new Gruppe(new GruppenId("GruppenId"),
                "mandant",
                "orgId",
                new GruppenId("referrerId"),
                "GruppeX",
                "ThemaX",
                "GruppeX sucks",
                Gruppentyp.KLASSE,
                Gruppenbereich.PFLICHT,
                Collections.singletonList(GruppenOption.BILINGUAL),
                GruppenDifferenzierung.E,
                Collections.singletonList(Bildungsziel.GS),
                Collections.singletonList(Jahrgangsstufe.JS_01),
                Collections.singletonList(new Fach("BI")),
                Collections.singletonList(
                                          new ReferenzGruppe(new GruppenId("ReferenzGruppenId"),
                                                  Collections.singletonList(Rolle.LEHR))),
                laufzeit,
                "Revision1");
        GruppeWithZugehoerigkeit gruppeWithZugehoerigkeit = new GruppeWithZugehoerigkeit(gruppe,
                new GruppenZugehoerigkeit(Collections.singletonList(Rolle.LEHR)));

        assertEquals("{\"gruppe\":{\"id\":\"GruppenId\",\"mandant\":\"mandant\",\"orgid\":\"orgId\",\"referrer\":\"referrerId\",\"bezeichnung\":\"GruppeX\",\"thema\":\"ThemaX\",\"beschreibung\":\"GruppeX sucks\",\"typ\":\"KLASSE\",\"bereich\":\"PFLICHT\",\"optionen\":[\"01\"],\"differenzierung\":\"E\",\"bildungsziele\":[\"GS\"],\"jahrgangsstufen\":[\"JS_01\"],\"faecher\":[{\"code\":\"BI\"}],\"referenzgruppen\":[{\"id\":\"ReferenzGruppenId\",\"rollen\":[\"LEHR\"]}],\"laufzeit\":{\"von\":\"2023-03-31\",\"bis\":\"2023-03-31\"},\"revision\":\"Revision1\"},\"gruppenzugehoerigkeit\":{\"rollen\":[\"LEHR\"]}}",
                     objectMapper.writeValueAsString(gruppeWithZugehoerigkeit));
    }

}