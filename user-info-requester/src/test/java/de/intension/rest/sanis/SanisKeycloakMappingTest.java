package de.intension.rest.sanis;

import com.google.common.io.Resources;
import de.intension.rest.IKeycloakApiMapper;
import org.junit.jupiter.api.Test;
import org.keycloak.broker.provider.BrokeredIdentityContext;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static de.intension.api.UserInfoAttribute.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SanisKeycloakMappingTest
{

    /**
     * GIVEN: JSON file with SANIS API structure
     * WHEN: mapping was executed
     * THEN: all configured SANIS fields must be mapped into standard user attributes
     */
    @Test
    void should_map_all_defined_sanis_input_fields_to_user_attributes()
        throws IOException
    {
        URL resource = Resources.getResource("de/intension/rest/sanis/UserInfo.json");
        String userInfoJson = Resources.toString(resource, StandardCharsets.UTF_8);
        BrokeredIdentityContext context = new BrokeredIdentityContext(null);
        IKeycloakApiMapper mapper = new SanisKeycloakMapping();
        mapper.addAttributesToResource(context, userInfoJson);
        assertEquals("Max", context.getUserAttribute(PERSON_VORNAME.getAttributeName()));
        assertEquals("M", context.getUserAttribute(PERSON_VORNAME_INITIALEN.getAttributeName()));
        assertEquals("Muster", context.getUserAttribute(PERSON_FAMILIENNAME.getAttributeName()));
        assertEquals("M", context.getUserAttribute(PERSON_FAMILIENNAME_INITIALEN.getAttributeName()));
        assertEquals("2010-01-01", context.getUserAttribute(PERSON_GEBURTSDATUM.getAttributeName()));
        assertEquals("Ostfildern, Deutschland", context.getUserAttribute(PERSON_GEBURTSORT.getAttributeName()));
        assertEquals("NEIN", context.getUserAttribute(PERSON_VOLLJAEHRIG.getAttributeName()));
        assertEquals("D", context.getUserAttribute(PERSON_GESCHLECHT.getAttributeName()));
        assertEquals("de-DE", context.getUserAttribute(PERSON_LOKALISIERUNG.getAttributeName()));
        assertEquals("VOLL", context.getUserAttribute(PERSON_VERTRAUENSSTUFE.getAttributeName()));
        assertEquals("af3a88fc-d766-11ec-9d64-0242ac120002", context.getUserAttribute(PERSON_KONTEXT_ARRAY_ID.getAttributeName().replace("#", "0")));
        assertEquals("15685758-d18e-49c1-a644-f9996eb0bf08", context.getUserAttribute(PERSON_KONTEXT_ARRAY_ORG_ID.getAttributeName().replace("#", "0")));
        assertEquals("NI_12345", context.getUserAttribute(PERSON_KONTEXT_ARRAY_ORG_KENNUNG.getAttributeName().replace("#", "0")));
        assertEquals("Muster-Schule", context.getUserAttribute(PERSON_KONTEXT_ARRAY_ORG_NAME.getAttributeName().replace("#", "0")));
        assertEquals("SCHULE", context.getUserAttribute(PERSON_KONTEXT_ARRAY_ORG_TYP.getAttributeName().replace("#", "0")));
        assertEquals("LERN", context.getUserAttribute(PERSON_KONTEXT_ARRAY_ROLLE.getAttributeName().replace("#", "0")));
        assertEquals("AKTIV", context.getUserAttribute(PERSON_KONTEXT_ARRAY_STATUS.getAttributeName().replace("#", "0")));
        assertEquals("2099-12-31T23:59Z", context.getUserAttribute(PERSON_KONTEXT_ARRAY_LOESCHUNG.getAttributeName().replace("#", "0")));
        assertEquals("{\"gruppe\":{\"id\":\"ab34d607-b950-41a5-b69d-80b8812c224a\",\"mandant\":\"02feb60dc3f691af4a4bf92410fac8292bb8e7d6adebb70b2a65d3c35d825d8a\",\"orgid\":\"02feb60dc3f691af4a4bf92410fac8292bb8e7d6adebb70b2a65d3c35d825d8a\",\"referrer\":\"fe4e50cb-c148-4156-8c2f-dc5260b267cf",
                     context.getUserAttribute(PERSON_KONTEXT_ARRAY_GRUPPEN.getAttributeName().replace("#", "0") + "[0]"));
        assertEquals("\",\"bezeichnung\":\"Englisch, 2. Klasse\",\"thema\":\"Thema\",\"beschreibung\":\"Beschreibung der Gruppe\",\"typ\":\"SONSTIG\",\"bereich\":\"WAHL\",\"optionen\":[\"01\",\"02\"],\"differenzierung\":\"G\",\"bildungsziele\":[\"GS\"],\"jahrgangsstufen\":[\"JS_02\"],\"faecher\":[{\"code\":\"EN\"}],\"refe",
                     context.getUserAttribute(PERSON_KONTEXT_ARRAY_GRUPPEN.getAttributeName().replace("#", "0") + "[0]_1"));
        assertEquals("renzgruppen\":[{\"id\":\"21252996-7a5d-47b5-9c62-c416460908f0\",\"rollen\":[\"LERN\",\"LEHR\"]}],\"laufzeit\":{\"von\":\"2023-08-01\",\"bis\":\"2024-01-31\",\"sichtfreigabe\":\"JA\"},\"revision\":\"1\"},\"gruppenzugehoerigkeit\":{\"rollen\":[\"LEHR\"]}}",
                     context.getUserAttribute(PERSON_KONTEXT_ARRAY_GRUPPEN.getAttributeName().replace("#", "0") + "[0]_2"));
        assertNotEquals("", context.getUserAttribute(PERSON_KONTEXT_GRUPPEN.getAttributeName().replace('#', '0') + "[1]"));
        assertEquals(24, context.getContextData().size());
    }
}