package de.intension.mapper.oidc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intension.api.UserInfoAttribute;

class RandomUserInfoProviderMapperTest
{

    private static final String       SUB          = "af3a88fc-d766-11ec-9d64-0242ac120002";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void should_not_add_claim_when_no_role_is_set()
    {
        RandomUserInfoProviderMapper mapper = new RandomUserInfoProviderMapper();
        IDToken idToken = new IDToken();
        idToken.setSubject(SUB);
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);
        TestUserModel user = createBaseUser();
        mapper.transformIDToken(idToken, createMapperModel(mapper), session, createUserSession(user), context);
        Assertions.assertNull(idToken.getOtherClaims().get("vidisInfo"));
    }

    @Test
    void should_add_claim_with_exactly_one_personenkontext_when_role_is_set()
        throws Exception
    {
        RandomUserInfoProviderMapper mapper = new RandomUserInfoProviderMapper();
        IDToken idToken = new IDToken();
        idToken.setSubject(SUB);
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);
        TestUserModel user = createBaseUser();
        user.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ROLLE.getAttributeName(), "LERN");
        mapper.transformIDToken(idToken, createMapperModel(mapper), session, createUserSession(user), context);
        String claim = (String)idToken.getOtherClaims().get("vidisInfo");
        Assertions.assertNotNull(claim);
        JsonNode node = objectMapper.readTree(claim);
        Assertions.assertEquals(1, node.get("personenkontexte").size());
        Assertions.assertEquals("LERN", node.get("personenkontexte").get(0).get("rolle").asText());
        Assertions.assertFalse(node.get("person").get("name").get("vorname").asText().isBlank());
        Assertions.assertFalse(node.get("personenkontexte").get(0).get("organisation").get("kennung").asText().isBlank());
        Assertions.assertEquals(1, node.get("personenkontexte").get(0).get("gruppen").size());
    }

    @Test
    void should_keep_real_attributes_and_fill_the_rest_deterministically()
        throws Exception
    {
        RandomUserInfoProviderMapper mapper = new RandomUserInfoProviderMapper();
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);

        TestUserModel user = createBaseUser();
        user.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ROLLE.getAttributeName(), "LERN");
        user.setSingleAttribute(UserInfoAttribute.PERSON_VORNAME.getAttributeName(), "Erika");
        UserSessionModel userSession = createUserSession(user);

        IDToken idToken1 = new IDToken();
        idToken1.setSubject(SUB);
        mapper.transformIDToken(idToken1, createMapperModel(mapper), session, userSession, context);
        JsonNode first = objectMapper.readTree((String)idToken1.getOtherClaims().get("vidisInfo"));

        IDToken idToken2 = new IDToken();
        idToken2.setSubject(SUB);
        mapper.transformIDToken(idToken2, createMapperModel(mapper), session, userSession, context);
        JsonNode second = objectMapper.readTree((String)idToken2.getOtherClaims().get("vidisInfo"));

        Assertions.assertEquals("Erika", first.get("person").get("name").get("vorname").asText());
        Assertions.assertEquals(first, second);
    }

    @Test
    void should_produce_different_data_for_different_underlying_attributes()
        throws Exception
    {
        RandomUserInfoProviderMapper mapper = new RandomUserInfoProviderMapper();
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);

        TestUserModel userA = createBaseUser();
        userA.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ROLLE.getAttributeName(), "LERN");
        TestUserModel userB = createBaseUser();
        userB.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ROLLE.getAttributeName(), "LEHR");

        IDToken idTokenA = new IDToken();
        idTokenA.setSubject(SUB);
        mapper.transformIDToken(idTokenA, createMapperModel(mapper), session, createUserSession(userA), context);
        JsonNode nodeA = objectMapper.readTree((String)idTokenA.getOtherClaims().get("vidisInfo"));

        IDToken idTokenB = new IDToken();
        idTokenB.setSubject(SUB);
        mapper.transformIDToken(idTokenB, createMapperModel(mapper), session, createUserSession(userB), context);
        JsonNode nodeB = objectMapper.readTree((String)idTokenB.getOtherClaims().get("vidisInfo"));

        Assertions.assertNotEquals(nodeA, nodeB);
    }

    private TestUserModel createBaseUser()
    {
        TestUserModel user = new TestUserModel(null, getTestRealm(), "1");
        user.setSingleAttribute(UserModel.IDP_ALIAS, "DE-SN-Schullogin");
        return user;
    }

    private ProtocolMapperModel createMapperModel(RandomUserInfoProviderMapper mapper)
    {
        ProtocolMapperModel protocolMapperModel = new ProtocolMapperModel();
        protocolMapperModel.setName(mapper.getDisplayType());
        Map<String, String> config = new HashMap<>();
        for (ProviderConfigProperty property : mapper.getConfigProperties()) {
            if (property.getName().equals(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME)) {
                config.put(property.getName(), "vidisInfo");
            }
            else if (property.getName().equals(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN)) {
                config.put(property.getName(), "true");
            }
            else if (property.getName().equals(OIDCAttributeMapperHelper.JSON_TYPE)) {
                config.put(property.getName(), "String");
            }
        }
        protocolMapperModel.setConfig(config);
        return protocolMapperModel;
    }

    private UserSessionModel createUserSession(UserModel user)
    {
        UserSessionModel userSessionModel = mock(UserSessionModel.class);
        RealmModel realm = getTestRealm();
        when(userSessionModel.getUser()).thenReturn(user);
        when(userSessionModel.getRealm()).thenReturn(realm);
        return userSessionModel;
    }

    private RealmModel getTestRealm()
    {
        RealmModel realm = mock(RealmModel.class);
        IdentityProviderModel idpModel = mock(IdentityProviderModel.class);
        when(idpModel.getAlias()).thenReturn("DE-SN-Schullogin");
        when(idpModel.getDisplayName()).thenReturn("Musterschule");
        when(realm.getIdentityProviderByAlias(anyString())).thenReturn(idpModel);
        return realm;
    }
}
