package de.intension.rest.sanis;

import static de.intension.api.UserInfoAttribute.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.keycloak.broker.provider.BrokeredIdentityContext;

import com.google.common.io.Resources;

import de.intension.rest.IKeycloakApiMapper;

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
        BrokeredIdentityContext context = new BrokeredIdentityContext("12345");
        IKeycloakApiMapper mapper = new SanisKeycloakMapping();
        mapper.addAttributesToResource(context, userInfoJson);
        assertEquals(13, context.getContextData().size());
        assertEquals("Max", context.getUserAttribute(PERSON_VORNAME.getAttributeName()));
        assertEquals("Muster", context.getUserAttribute(PERSON_FAMILIENNAME.getAttributeName()));
        assertEquals("2010-01-01", context.getUserAttribute(PERSON_GEBURTSDATUM.getAttributeName()));
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
    }
}