package de.intension.mapper.oidc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import de.intension.api.UserInfoAttribute;

class UserInfoProviderMapperTest
{

    private static final String SUB = "af3a88fc-d766-11ec-9d64-0242ac120002";

    @Test
    void should_map_all_user_attributes_to_userInfo_claim()
        throws URISyntaxException, IOException, JSONException
    {
        UserInfoProviderMapper mapper = new UserInfoProviderMapper();
        IDToken idToken = new IDToken();
        idToken.setSubject(SUB);
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);
        mapper.transformIDToken(idToken, createMapperModel(mapper), session, createUserModel(), context);
        String userInfo = (String)idToken.getOtherClaims().get("userInfo");
        Assertions.assertNotNull(userInfo);
        JSONAssert.assertEquals(getJsonResourceAsString("de/intension/mapper/oidc/UserInfo.json"), userInfo, JSONCompareMode.STRICT);
    }

    @Test
    void should_map_all_default_user_attributes_to_userInfo_claim()
        throws URISyntaxException, IOException, JSONException
    {
        UserInfoProviderMapper mapper = new UserInfoProviderMapper();
        IDToken idToken = new IDToken();
        idToken.setSubject(SUB);
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);
        mapper.transformIDToken(idToken, createMapperModel(mapper), session, createDefaultUserModel(), context);
        String userInfo = (String)idToken.getOtherClaims().get("userInfo");
        Assertions.assertNotNull(userInfo);
        JSONAssert.assertEquals(getJsonResourceAsString("de/intension/mapper/oidc/UserInfoDefault.json"), userInfo, JSONCompareMode.STRICT);
    }

    private ProtocolMapperModel createMapperModel(UserInfoProviderMapper mapper)
    {
        ProtocolMapperModel protocolMapperModel = new ProtocolMapperModel();
        protocolMapperModel.setName(mapper.getDisplayType());
        Map<String, String> config = new HashMap<>();
        for (ProviderConfigProperty property : mapper.getConfigProperties()) {
            String propertyName = property.getName();
            if (propertyName.startsWith("heimatorganisation.") || propertyName.startsWith("person.")) {
                config.put(propertyName, "true");
            }
            else if (propertyName.equals(UserInfoProviderMapper.USER_INFO_ATTRIBUTE_NAME)) {
                config.put(propertyName, "userInfo");
            }
            else if (propertyName.equals(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN)) {
                config.put(propertyName, "true");
            }
            else if (propertyName.equals(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME)) {
                config.put(propertyName, "userInfo");
            }
        }
        protocolMapperModel.setConfig(config);
        return protocolMapperModel;
    }

    private UserSessionModel createUserModel()
    {
        UserSessionModel userSessionModel = mock(UserSessionModel.class);
        TestUserModel userModel = new TestUserModel(null, null, "224");
        userModel.setSingleAttribute(UserInfoAttribute.HEIMATORGANISATION_ID.getAttributeName(), "3e3b679c-6cae-11ed-a1eb-0242ac120002");
        userModel.setSingleAttribute(UserInfoAttribute.HEIMATORGANISATION_NAME.getAttributeName(), "DE-SN-Schullogin");
        userModel.setSingleAttribute(UserInfoAttribute.HEIMATORGANISATION_BUNDESLAND.getAttributeName(), "DE-BY");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_FAMILIENNAME.getAttributeName(), "Muster");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_VORNAME.getAttributeName(), "Max");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_AKRONYM.getAttributeName(), "MaMu");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_GEBURTSDATUM.getAttributeName(), "2010-01-01");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_GESCHLECHT.getAttributeName(), "D");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_LOKALISIERUNG.getAttributeName(), "de-DE");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_VERTRAUENSSTUFE.getAttributeName(), "VOLL");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ORG_KENNUNG.getAttributeName(), "5555");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ORG_NAME.getAttributeName(), "Test-Schule");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ORG_TYP.getAttributeName(), "ANBIETER");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ROLLE.getAttributeName(), "LEHR");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_STATUS.getAttributeName(), "INAKTIV");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ORG_VIDIS_ID.getAttributeName(), "vidis.id");
        userModel.setSingleAttribute(getArrayAttName(UserInfoAttribute.PERSON_KONTEXT_ARRAY_ORG_KENNUNG, 0), "NI_12345");
        userModel.setSingleAttribute(getArrayAttName(UserInfoAttribute.PERSON_KONTEXT_ARRAY_ORG_NAME, 0), "Muster-Schule");
        userModel.setSingleAttribute(getArrayAttName(UserInfoAttribute.PERSON_KONTEXT_ARRAY_ORG_TYP, 0), "SCHULE");
        userModel.setSingleAttribute(getArrayAttName(UserInfoAttribute.PERSON_KONTEXT_ARRAY_ROLLE, 0), "LERN");
        userModel.setSingleAttribute(getArrayAttName(UserInfoAttribute.PERSON_KONTEXT_ARRAY_STATUS, 0), "AKTIV");
        userModel.setReadonly(false);
        when(userSessionModel.getUser()).thenReturn(userModel);
        return userSessionModel;
    }

    private UserSessionModel createDefaultUserModel()
    {
        UserSessionModel userSessionModel = mock(UserSessionModel.class);
        TestUserModel userModel = new TestUserModel(null, null, "224");
        userModel.setSingleAttribute(UserInfoAttribute.HEIMATORGANISATION_ID.getAttributeName(), "3e3b679c-6cae-11ed-a1eb-0242ac120002");
        userModel.setSingleAttribute(UserInfoAttribute.HEIMATORGANISATION_NAME.getAttributeName(), "DE-SN-Schullogin");
        userModel.setSingleAttribute(UserInfoAttribute.HEIMATORGANISATION_BUNDESLAND.getAttributeName(), "DE-BY");
        userModel.setFirstName("Max");
        userModel.setLastName("Muster");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ORG_KENNUNG.getAttributeName(), "5555");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ORG_NAME.getAttributeName(), "Test-Schule");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ROLLE.getAttributeName(), "LEHR");
        userModel.setSingleAttribute(getArrayAttName(UserInfoAttribute.PERSON_KONTEXT_ARRAY_ORG_KENNUNG, 0), "NI_12345");
        userModel.setSingleAttribute(getArrayAttName(UserInfoAttribute.PERSON_KONTEXT_ARRAY_ORG_NAME, 0), "Muster-Schule");
        userModel.setSingleAttribute(getArrayAttName(UserInfoAttribute.PERSON_KONTEXT_ARRAY_ROLLE, 0), "LERN");
        userModel.setReadonly(false);
        when(userSessionModel.getUser()).thenReturn(userModel);
        return userSessionModel;
    }

    private String getArrayAttName(UserInfoAttribute uia, int index)
    {
        return uia.getAttributeName().replace("#", String.valueOf(index));
    }

    /**
     * Get JSON string from resource path.
     */
    private String getJsonResourceAsString(String path)
        throws URISyntaxException, IOException
    {
        URL url = getClass().getClassLoader().getResource(path);
        return new String(Files.readAllBytes(Paths.get(url.toURI())));
    }
}