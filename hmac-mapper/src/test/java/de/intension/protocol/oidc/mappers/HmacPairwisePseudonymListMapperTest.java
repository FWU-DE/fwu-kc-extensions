package de.intension.protocol.oidc.mappers;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.intension.protocol.oidc.mappers.HmacPairwisePseudonymListMapper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HmacPairwisePseudonymListMapperTest
{

    public static final String              NON_EXISTING_CLIENT = "non-existing-client";
    private static final String             EXISTING_CLIENT     = "existing-client";
    private final HmacPairwisePseudonymListMapper classUnderTest      = new HmacPairwisePseudonymListMapper();

    @Mock
    private KeycloakSession                 sessionMock;
    @Mock
    private RealmModel                      realmMock;
    @Mock
    private ProtocolMapperModel             mapperMock;
    @Mock
    private KeycloakContext                 contextMock;

    @Test
    void shouldFail_whenValidateConfig_givenClientNotExists()
    {
        //given
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CLIENTS_PROP_NAME, EXISTING_CLIENT + " , " + NON_EXISTING_CLIENT);
        when(mapperMock.getConfig()).thenReturn(configMap);

        ClientModel existingClient = mock(ClientModel.class);
        ProtocolMapperModel referencedClientMapper = mock(ProtocolMapperModel.class);
        when(referencedClientMapper.getProtocolMapper()).thenReturn(HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID);
        when(existingClient.getProtocolMappersStream()).thenReturn(Stream.of(referencedClientMapper));

        when(sessionMock.getContext()).thenReturn(contextMock);
        when(contextMock.getRealm()).thenReturn(realmMock);
        when(realmMock.getClientByClientId(EXISTING_CLIENT)).thenReturn(existingClient);
        when(realmMock.getClientByClientId(NON_EXISTING_CLIENT)).thenReturn(null);
        //when
        ProtocolMapperContainerModel clientModel = mock(ClientModel.class);
        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                                                               () -> classUnderTest.validateConfig(sessionMock, realmMock, clientModel, mapperMock));
        //then
        assertThat((String)exception.getParameters()[0], Matchers.equalTo(NON_EXISTING_CLIENT));
        assertThat(exception.getMessageKey(), Matchers.equalTo(HmacPairwisePseudonymListMapper.CLIENT_DOES_NOT_EXIST_MSG_KEY));
    }
    @Test
    void shouldFail_whenValidateConfig_givenClaimNotSet()
    {
        //given
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CLIENTS_PROP_NAME, EXISTING_CLIENT);
        when(mapperMock.getConfig()).thenReturn(configMap);

        ClientModel existingClient = mock(ClientModel.class);
        ProtocolMapperModel referencedClientMapper = mock(ProtocolMapperModel.class);
        when(referencedClientMapper.getProtocolMapper()).thenReturn(HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID);
        when(existingClient.getProtocolMappersStream()).thenReturn(Stream.of(referencedClientMapper));

        when(sessionMock.getContext()).thenReturn(contextMock);
        when(contextMock.getRealm()).thenReturn(realmMock);
        when(realmMock.getClientByClientId(EXISTING_CLIENT)).thenReturn(existingClient);
        //when
        ProtocolMapperContainerModel clientModel = mock(ClientModel.class);
        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                                                               () -> classUnderTest.validateConfig(sessionMock, realmMock, clientModel, mapperMock));
        //then
        assertThat(exception.getMessageKey(), Matchers.equalTo(TARGET_CLAIM_NOT_SET_MSG_KEY));
    }

    @Test
    void shouldFail_whenValidateConfig_givenReferencedClientOfFalseType()
    {
        //given
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CLIENTS_PROP_NAME, EXISTING_CLIENT);
        when(mapperMock.getConfig()).thenReturn(configMap);

        ClientModel existingClient = mock(ClientModel.class);
        ProtocolMapperModel referencedClientMapper = mock(ProtocolMapperModel.class);
        String incorrectProtcollMapperId = "incorrectProtocolMapper";
        when(referencedClientMapper.getProtocolMapper()).thenReturn(incorrectProtcollMapperId);
        when(existingClient.getProtocolMappersStream()).thenReturn(Stream.of(referencedClientMapper));

        when(sessionMock.getContext()).thenReturn(contextMock);
        when(contextMock.getRealm()).thenReturn(realmMock);
        when(realmMock.getClientByClientId(EXISTING_CLIENT)).thenReturn(existingClient);
        //when
        ProtocolMapperContainerModel clientModel = mock(ClientModel.class);
        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                                                               () -> classUnderTest.validateConfig(sessionMock, realmMock, clientModel, mapperMock));
        //then
        assertThat((String)exception.getParameters()[0], Matchers.equalTo(EXISTING_CLIENT));
        assertThat(exception.getMessageKey(), Matchers.equalTo(HmacPairwisePseudonymListMapper.CLIENT_DOES_NOT_EXIST_MSG_KEY));
    }

    @Test
    void shouldSucceed_whenValidateConfig_givenCorrectConfig()
        throws ProtocolMapperConfigException
    {
        //given
        Map<String, String> configMap = new HashMap<>();
        configMap.put(CLIENTS_PROP_NAME, EXISTING_CLIENT);
        configMap.put(CLAIM_PROP_NAME, "otherPseudonyms");
        when(mapperMock.getConfig()).thenReturn(configMap);

        ClientModel existingClient = mock(ClientModel.class);
        ProtocolMapperModel referencedClientMapper = mock(ProtocolMapperModel.class);
        when(referencedClientMapper.getProtocolMapper()).thenReturn(HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID);
        when(existingClient.getProtocolMappersStream()).thenReturn(Stream.of(referencedClientMapper));

        when(sessionMock.getContext()).thenReturn(contextMock);
        when(contextMock.getRealm()).thenReturn(realmMock);
        when(realmMock.getClientByClientId(EXISTING_CLIENT)).thenReturn(existingClient);
        //when
        ProtocolMapperContainerModel clientModel = mock(ClientModel.class);
        classUnderTest.validateConfig(sessionMock, realmMock, clientModel, mapperMock);
    }

    @Test
    void shouldFail_whenValidateConfig_givenConfiguredForNonClientMapper()
    {
        //given
        ProtocolMapperContainerModel incorrectMapperContainerType = mock(ProtocolMapperContainerModel.class);
        //when
        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                                                               () -> classUnderTest.validateConfig(sessionMock, realmMock, incorrectMapperContainerType,
                                                                                                   mapperMock));
        //then
        assertThat((String)exception.getParameters()[0], Matchers.equalTo(HmacPairwisePseudonymListMapper.ACCEPTED_MAPPER_TYPE));
        assertThat(exception.getMessageKey(), Matchers.equalTo(HmacPairwisePseudonymListMapper.WRONG_MAPPER_TYPE_MSG_KEY));
    }

    @ParameterizedTest
    @MethodSource("provideTokenTransformMethodAndTokenType")
    void shouldNotModifyToken_whenTransformIDToken_givenIncludeInIdTokenFalse(String transformMethodName, Class<?> tokenClazz)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        //given
        AccessToken idToken = new AccessToken(); //AccessToken inherits IdToken so we can just use AccessToken for all 3 transformations
        UserSessionModel userSession = mock(UserSessionModel.class);
        ClientSessionContext clientSessionContext = mock(ClientSessionContext.class);
        when(mapperMock.getConfig()).thenReturn(new HashMap<>());

        //when
        Method transformTokenMethod = classUnderTest.getClass().getDeclaredMethod(transformMethodName, tokenClazz, ProtocolMapperModel.class,
                                                                                  KeycloakSession.class, UserSessionModel.class, ClientSessionContext.class);
        transformTokenMethod.invoke(classUnderTest, idToken, mapperMock, sessionMock, userSession, clientSessionContext);
        //then
        assertThat(idToken.getOtherClaims(), Matchers.anEmptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideTokenTransformMethodAndTokenType")
    void shouldNotModifyToken_whenTransformIDToken_givenClientRemoved(String transformMethodName, Class<?> tokenClazz)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        //given
        AccessToken idToken = new AccessToken();
        UserSessionModel userSession = mock(UserSessionModel.class);
        ClientSessionContext clientSessionContext = mock(ClientSessionContext.class);
        String additionalClaim = "otherPseudonyms";
        HashMap<String, String> configMap = new HashMap<>();
        configMap.put(HmacPairwisePseudonymListMapper.CLAIM_PROP_NAME, additionalClaim);
        configMap.put(CLIENTS_PROP_NAME, NON_EXISTING_CLIENT);
        configMap.put(INCLUDE_IN_ID_TOKEN, "true");
        configMap.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        configMap.put(INCLUDE_IN_USERINFO, "true");

        when(mapperMock.getConfig()).thenReturn(configMap);
        when(mapperMock.getName()).thenReturn("DifferentClient");
        when(sessionMock.getContext()).thenReturn(contextMock);
        when(contextMock.getRealm()).thenReturn(realmMock);
        when(realmMock.getClientByClientId(NON_EXISTING_CLIENT)).thenReturn(null);

        //when
        Method transformTokenMethod = classUnderTest.getClass().getDeclaredMethod(transformMethodName, tokenClazz, ProtocolMapperModel.class,
                                                                                  KeycloakSession.class, UserSessionModel.class, ClientSessionContext.class);
        transformTokenMethod.invoke(classUnderTest, idToken, mapperMock, sessionMock, userSession, clientSessionContext);
        //then
        assertThat(idToken.getOtherClaims(), Matchers.anEmptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideTokenTransformMethodAndTokenType")
    void shouldNotModifyToken_whenTransformToken_givenSubjectNull(String transformMethodName, Class<?> tokenClazz)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        //given
        AccessToken idToken = new AccessToken();
        UserSessionModel userSession = mock(UserSessionModel.class);
        UserModel userMock = mock(UserModel.class);
        when(userSession.getUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn(null);
        ClientSessionContext clientSessionContext = mock(ClientSessionContext.class);

        String additionalClaim = "otherPseudonyms";
        HashMap<String, String> configMap = new HashMap<>();
        configMap.put(HmacPairwisePseudonymListMapper.CLAIM_PROP_NAME, additionalClaim);
        configMap.put(CLIENTS_PROP_NAME, EXISTING_CLIENT);
        configMap.put(INCLUDE_IN_ID_TOKEN, "true");
        configMap.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        configMap.put(INCLUDE_IN_USERINFO, "true");

        when(mapperMock.getConfig()).thenReturn(configMap);
        when(sessionMock.getContext()).thenReturn(contextMock);
        when(contextMock.getRealm()).thenReturn(realmMock);

        ClientModel existingClient = mock(ClientModel.class);
        ProtocolMapperModel referencedClientMapper = mock(ProtocolMapperModel.class);
        when(referencedClientMapper.getProtocolMapper()).thenReturn(HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID);

        Map<String, String> referencedClientConfig = new HashMap<>();
        referencedClientConfig.put(HmacPairwiseSubMapperHelper.LOCAL_SUB_IDENTIFIER_PROP_NAME, "id");
        when(referencedClientMapper.getConfig()).thenReturn(referencedClientConfig);
        when(existingClient.getProtocolMappersStream()).thenReturn(Stream.of(referencedClientMapper));
        when(realmMock.getClientByClientId(EXISTING_CLIENT)).thenReturn(existingClient);

        //when
        Method transformTokenMethod = classUnderTest.getClass().getDeclaredMethod(transformMethodName, tokenClazz, ProtocolMapperModel.class,
                                                                                  KeycloakSession.class, UserSessionModel.class, ClientSessionContext.class);
        transformTokenMethod.invoke(classUnderTest, idToken, mapperMock, sessionMock, userSession, clientSessionContext);
        //then
        assertThat(idToken.getOtherClaims(), Matchers.anEmptyMap());
    }

    @ParameterizedTest
    @MethodSource("provideTokenTransformMethodAndTokenType")
    void shouldSetPseudonymList_whenTransformToken_givenClaimNotSet(String transformMethodName, Class<?> tokenClazz)
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        //given
        AccessToken idToken = new AccessToken();
        UserSessionModel userSession = mock(UserSessionModel.class);
        UserModel userMock = mock(UserModel.class);
        when(userSession.getUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn("userId");
        ClientSessionContext clientSessionContext = mock(ClientSessionContext.class);

        String additionalClaim = "otherPseudonyms";
        HashMap<String, String> configMap = new HashMap<>();
        configMap.put(HmacPairwisePseudonymListMapper.CLAIM_PROP_NAME, additionalClaim);
        configMap.put(CLIENTS_PROP_NAME, EXISTING_CLIENT);
        configMap.put(INCLUDE_IN_ID_TOKEN, "true");
        configMap.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        configMap.put(INCLUDE_IN_USERINFO, "true");

        when(mapperMock.getConfig()).thenReturn(configMap);
        when(sessionMock.getContext()).thenReturn(contextMock);
        when(contextMock.getRealm()).thenReturn(realmMock);

        ClientModel existingClient = mock(ClientModel.class);
        ProtocolMapperModel referencedClientMapper = mock(ProtocolMapperModel.class);
        when(referencedClientMapper.getProtocolMapper()).thenReturn(HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID);

        Map<String, String> referencedClientConfig = new HashMap<>();
        referencedClientConfig.put(HmacPairwiseSubMapperHelper.LOCAL_SUB_IDENTIFIER_PROP_NAME, "id");
        referencedClientConfig.put(PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT, "123456");
        referencedClientConfig.put(HmacPairwiseSubMapperHelper.HASH_ALGORITHM_PROP_NAME, "HmacSHA384");
        referencedClientConfig.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, "https://example.com");
        when(referencedClientMapper.getConfig()).thenReturn(referencedClientConfig);
        when(existingClient.getProtocolMappersStream()).thenReturn(Stream.of(referencedClientMapper));
        when(realmMock.getClientByClientId(EXISTING_CLIENT)).thenReturn(existingClient);

        //when
        Method transformTokenMethod = classUnderTest.getClass().getDeclaredMethod(transformMethodName, tokenClazz, ProtocolMapperModel.class,
                                                                                  KeycloakSession.class, UserSessionModel.class, ClientSessionContext.class);
        transformTokenMethod.invoke(classUnderTest, idToken, mapperMock, sessionMock, userSession, clientSessionContext);

        //then
        assertThat(idToken.getOtherClaims(), Matchers.aMapWithSize(1));
        assertThat(idToken.getOtherClaims().get(additionalClaim), Matchers.instanceOf(Map.class));
        Map<String, String> actualClaim = (Map<String, String>)idToken.getOtherClaims().get(additionalClaim);
        assertThat(actualClaim.get(EXISTING_CLIENT), Matchers.not(Matchers.emptyString()));
    }

    @ParameterizedTest
    @MethodSource("provideTokenTransformMethodAndTokenType")
    void shouldAddPseudonymToList_whenTransformToken_givenClaimAlreadySet(String transformMethodName, Class tokenClazz)
        throws InvocationTargetException, IllegalAccessException, NoSuchMethodException
    {

        //given
        AccessToken idToken = new AccessToken();
        String additionalClaim = "otherPseudonyms";
        HashMap existingClaim = new HashMap<>();
        String differentClient = "client";
        existingClaim.put(differentClient, "pseudonym");
        idToken.getOtherClaims().put(additionalClaim, existingClaim);
        UserSessionModel userSession = mock(UserSessionModel.class);
        UserModel userMock = mock(UserModel.class);
        when(userSession.getUser()).thenReturn(userMock);
        when(userMock.getId()).thenReturn("userId");
        ClientSessionContext clientSessionContext = mock(ClientSessionContext.class);

        HashMap<String, String> configMap = new HashMap<>();
        configMap.put(HmacPairwisePseudonymListMapper.CLAIM_PROP_NAME, additionalClaim);
        configMap.put(CLIENTS_PROP_NAME, EXISTING_CLIENT);
        configMap.put(INCLUDE_IN_ID_TOKEN, "true");
        configMap.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        configMap.put(INCLUDE_IN_USERINFO, "true");

        when(mapperMock.getConfig()).thenReturn(configMap);
        when(sessionMock.getContext()).thenReturn(contextMock);
        when(contextMock.getRealm()).thenReturn(realmMock);

        ClientModel existingClient = mock(ClientModel.class);
        ProtocolMapperModel referencedClientMapper = mock(ProtocolMapperModel.class);
        when(referencedClientMapper.getProtocolMapper()).thenReturn(HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID);

        Map<String, String> referencedClientConfig = new HashMap<>();
        referencedClientConfig.put(HmacPairwiseSubMapperHelper.LOCAL_SUB_IDENTIFIER_PROP_NAME, "id");
        referencedClientConfig.put(PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT, "123456");
        referencedClientConfig.put(HmacPairwiseSubMapperHelper.HASH_ALGORITHM_PROP_NAME, "HmacSHA384");
        referencedClientConfig.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, "https://example.com");
        when(referencedClientMapper.getConfig()).thenReturn(referencedClientConfig);
        when(existingClient.getProtocolMappersStream()).thenReturn(Stream.of(referencedClientMapper));
        when(realmMock.getClientByClientId(EXISTING_CLIENT)).thenReturn(existingClient);

        //when
        Method transformTokenMethod = classUnderTest.getClass().getDeclaredMethod(transformMethodName, tokenClazz, ProtocolMapperModel.class,
                                                                                  KeycloakSession.class, UserSessionModel.class, ClientSessionContext.class);
        transformTokenMethod.invoke(classUnderTest, idToken, mapperMock, sessionMock, userSession, clientSessionContext);

        //then
        assertThat(idToken.getOtherClaims(), Matchers.aMapWithSize(1));
        assertThat(idToken.getOtherClaims().get(additionalClaim), Matchers.instanceOf(Map.class));
        Map<String, String> actualClaim = (Map<String, String>)idToken.getOtherClaims().get(additionalClaim);
        assertThat(actualClaim, Matchers.aMapWithSize(2));
        assertThat(actualClaim, Matchers.hasKey(EXISTING_CLIENT));
        assertThat(actualClaim, Matchers.hasKey(differentClient));
    }

    private static Stream<Arguments> provideTokenTransformMethodAndTokenType()
    {
        return Stream.of(
                         Arguments.of("transformIDToken", IDToken.class),
                         Arguments.of("transformAccessToken", AccessToken.class),
                         Arguments.of("transformUserInfoToken", AccessToken.class));
    }

    @Test
    void shouldProvideCorrectConfig_whenGetConfigProperties()
    {
        //given
        //when
        List<ProviderConfigProperty> configProperties = classUnderTest.getConfigProperties();
        //then
        assertThat(configProperties.stream().map(ProviderConfigProperty::getName).collect(Collectors.toList()),
                   Matchers.containsInAnyOrder(CLIENTS_PROP_NAME, CLAIM_PROP_NAME, INCLUDE_IN_ACCESS_TOKEN, INCLUDE_IN_ID_TOKEN,
                                               INCLUDE_IN_USERINFO, INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN));
    }
}