package de.intension.mapper.oidc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.IDToken;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.intension.api.UserInfoAttribute;
import de.intension.mapper.user.UserInfoHelper;

public class RoleBasedUserInfoProviderMapperTest
{

    private static final String       SUB          = "af3a88fc-d766-11ec-9d64-0242ac120002";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void setupObjectMapper()
    {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @ParameterizedTest
    @ValueSource(strings = {"LEHR", "LEHR,EXTERN"})
    void should_map_first_name_to_claim(String roleName)
        throws IOException
    {
        RoleBasedUserInfoProviderMapper mapper = new RoleBasedUserInfoProviderMapper();
        IDToken idToken = new IDToken();
        idToken.setSubject(SUB);
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);
        mapper.transformIDToken(idToken, createMapperModel(mapper, roleName, false), session, createUserModel("LEHR"), context);
        String userInfo = (String)idToken.getOtherClaims().get("userNameCustom");
        Assertions.assertNotNull(userInfo);
        Assertions.assertEquals("testFirstName", userInfo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_ROLE", "NO_ROLE,EXTERN"})
    void should_not_map_first_name_to_claim(String roleName)
        throws IOException
    {
        RoleBasedUserInfoProviderMapper mapper = new RoleBasedUserInfoProviderMapper();
        IDToken idToken = new IDToken();
        idToken.setSubject(SUB);
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);
        mapper.transformIDToken(idToken, createMapperModel(mapper, roleName, false), session, createUserModel("LEHR"), context);
        String userInfo = (String)idToken.getOtherClaims().get("userNameCustom");
        Assertions.assertNull(userInfo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"NO_ROLE", "NO_ROLE,EXTERN"})
    void should_map_first_name_with_negate_output_to_claim(String roleName)
        throws IOException
    {
        RoleBasedUserInfoProviderMapper mapper = new RoleBasedUserInfoProviderMapper();
        IDToken idToken = new IDToken();
        idToken.setSubject(SUB);
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);
        mapper.transformIDToken(idToken, createMapperModel(mapper, roleName, true), session, createUserModel("LEHR"), context);
        String userInfo = (String)idToken.getOtherClaims().get("userNameCustom");
        Assertions.assertNotNull(userInfo);
        Assertions.assertEquals("testFirstName", userInfo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LEHR", "LEHR,EXTERN"})
    void should_not_map_first_name_with_negate_output_to_claim(String roleName)
        throws IOException
    {
        RoleBasedUserInfoProviderMapper mapper = new RoleBasedUserInfoProviderMapper();
        IDToken idToken = new IDToken();
        idToken.setSubject(SUB);
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);
        mapper.transformIDToken(idToken, createMapperModel(mapper, roleName, true), session, createUserModel("LEHR"), context);
        String userInfo = (String)idToken.getOtherClaims().get("userNameCustom");
        Assertions.assertNull(userInfo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LEHR,EXTERN"})
    void should_test_multi_value_first_name_to_claim(String roleName)
        throws IOException
    {
        RoleBasedUserInfoProviderMapper mapper = new RoleBasedUserInfoProviderMapper();
        IDToken idToken = new IDToken();
        idToken.setSubject(SUB);
        KeycloakSession session = mock(KeycloakSession.class);
        ClientSessionContext context = mock(ClientSessionContext.class);
        mapper.transformIDToken(idToken, createMapperModel(mapper, roleName, false), session, createUserModel("EXTERN"), context);
        String userInfo = (String)idToken.getOtherClaims().get("userNameCustom");
        Assertions.assertNotNull(userInfo);
        Assertions.assertEquals("testFirstName", userInfo);
    }

    private ProtocolMapperModel createMapperModel(RoleBasedUserInfoProviderMapper mapper, String role, boolean negateOutput)
    {
        ProtocolMapperModel protocolMapperModel = new ProtocolMapperModel();
        protocolMapperModel.setName(mapper.getDisplayType());
        Map<String, String> config = new HashMap<>();
        config.put("user.attribute", "firstName");
        config.put("professionalRoles", role);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "userNameCustom");
        config.put("negateOutput", Boolean.toString(negateOutput));
        protocolMapperModel.setConfig(config);
        return protocolMapperModel;
    }

    private UserSessionModel createUserModel(String roleName)
        throws IOException
    {
        UserSessionModel userSessionModel = mock(UserSessionModel.class);
        RealmModel realm = getTestRealm();
        TestUserModel userModel = new TestUserModel(null, realm, "224");
        userModel.setFirstName("testFirstName");
        userModel.setSingleAttribute(UserModel.IDP_ALIAS, "DE-SN-Schullogin");
        userModel.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ROLLE.getAttributeName(), roleName);
        userModel.setSingleAttribute(UserInfoHelper.getIndexedAttributeName(UserInfoAttribute.PERSON_KONTEXT_ARRAY_ROLLE, 0), "LERN");
        when(userSessionModel.getUser()).thenReturn(userModel);
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
