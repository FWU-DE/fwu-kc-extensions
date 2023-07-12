package de.intension.authentication.schools;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.models.*;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.MediaType;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {18733})
@SuppressWarnings("java:S5976")
class SchoolWhitelistAuthenticatorTest
{

    private final ClientAndServer clientAndServer;

    private final static String   WHITELIST_DEDICATED         = "{\"allowAll\": false,\"vidisSchoolIdentifiers\": [\"1234\"]}";
    private final static String   WHITELIST_ALLOW_ALL         = "{\"allowAll\": true,\"vidisSchoolIdentifiers\": []}";
    private final static String   WHITELIST_NOT_ENTRIES_FOUND = "{\"allowAll\": false,\"vidisSchoolIdentifiers\": []}";
    private final static String   IDP_VALID                   = "validIdP";
    private final static String   IDP_INVALID                 = "invalidIdP";

    public SchoolWhitelistAuthenticatorTest(ClientAndServer client)
    {
        clientAndServer = client;
        initMockServer();
    }

    /**
     * Add expectation to mock server.
     */
    void initMockServer()
    {
        clientAndServer
            .when(
                  request().withPath("/school-assignments")
                      .withQueryStringParameter("serviceProvider", "test-client")
                      .withQueryStringParameter("idpId", IDP_VALID))
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withHeaders(
                                      header(CONTENT_TYPE.toString(), MediaType.JSON_UTF_8.getType()))
                         .withBody(WHITELIST_DEDICATED));
        clientAndServer
            .when(
                  request().withPath("/school-assignments")
                      .withQueryStringParameter("serviceProvider", "allow-all-client")
                      .withQueryStringParameter("idpId", IDP_VALID))
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withHeaders(
                                      header(CONTENT_TYPE.toString(), MediaType.JSON_UTF_8.getType()))
                         .withBody(WHITELIST_ALLOW_ALL));
        clientAndServer
            .when(
                  request().withPath("/school-assignments")
                      .withQueryStringParameter("serviceProvider", "not-configured-client")
                      .withQueryStringParameter("idpId", IDP_VALID))
            .respond(
                     response()
                         .withBody(WHITELIST_NOT_ENTRIES_FOUND));
        clientAndServer
            .when(
                  request().withPath("/school-assignments")
                      .withQueryStringParameter("serviceProvider", "test-client")
                      .withQueryStringParameter("idpId", IDP_INVALID))
            .respond(
                     response()
                         .withBody(WHITELIST_NOT_ENTRIES_FOUND));
        clientAndServer
            .when(
                  request().withPath("/auth/realms/test/protocol/openid-connect/token"))
            .respond(
                     response()
                         .withBody("{\"access_token\":\"12345\"}"));
    }

    /**
     * GIVEN: valid whitelist configuration which is reachable via URI
     * WHEN: authentication flow is called with a configured clientId and valid schoolId user attribute
     * THEN: context status is "success"
     */
    @ParameterizedTest
    @CsvSource({LoginActionsService.FIRST_BROKER_LOGIN_PATH, LoginActionsService.POST_BROKER_LOGIN_PATH})
    void should_permit_access_because_of_matching_clientId_and_schoolId(String flowPath)
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234"), IDP_VALID, flowPath);
        authenticator.authenticate(context);
        assertEquals(true, context.getSuccess());
    }

    /**
     * GIVEN: valid whitelist configuration which is reachable via URI
     * WHEN: authentication flow is called with a configured clientId and a list of valid schoolIds
     * THEN: context status is "success"
     */
    @Test
    void should_permit_access_because_of_matching_clientId_and_schoolId_inside_a_list()
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234", "5678"));
        authenticator.authenticate(context);
        assertEquals(true, context.getSuccess());
    }

    /**
     * GIVEN: valid whitelist configuration which is reachable via URI
     * WHEN: authentication flow is called with a configured clientId and User with an invalid schoolId
     * THEN: context status is "failed"
     */
    @Test
    void should_deny_access_because_of_invalid_schoolId()
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234x"));
        authenticator.authenticate(context);
        assertEquals(false, context.getSuccess());
    }

    /**
     * GIVEN: valid whitelist configuration which is reachable via URI
     * WHEN: authentication flow is called with a configured clientId and User with an empty schoolId
     * attribute
     * THEN: context status is "failed"
     */
    @Test
    void should_deny_access_because_of_empty_schoolId_user_attribute()
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", new ArrayList<>());
        authenticator.authenticate(context);
        assertEquals(false, context.getSuccess());
    }

    /**
     * GIVEN: valid whitelist configuration which is reachable via URI
     * WHEN: authentication flow is called with a not configured clientId and valid schoolId user
     * attribute
     * THEN: context status is "failed"
     */
    @Test
    void should_deny_access_because_of_a_not_configured_clientId()
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("not-configured-client", List.of("1234"));
        authenticator.authenticate(context);
        assertEquals(false, context.getSuccess());
    }

    /**
     * GIVEN: a valid whitelist configuration with a client configured with ALLOW_ALL marker
     * WHEN: authentication flow is called with this client and any school id in the user attribute
     * THEN: context status is "success"
     */
    @Test
    void should_allow_access_because_of_allow_all_marker()
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("allow-all-client", List.of("1234"));
        authenticator.authenticate(context);
        assertEquals(true, context.getSuccess());
    }

    /**
     * GIVEN: whitelist configuration which is not accessible via URI
     * WHEN: authentication flow is called with a configured clientId and valid schoolId user attribute
     * THEN: context status is "failed"
     */
    @Test
    void should_deny_access_because_of_invalid_whitelist_URI()
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator("http://invalid:18733/school-assignments");
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234"));
        authenticator.authenticate(context);
        assertEquals(false, context.getSuccess());
    }

    /**
     * GIVEN: valid whitelist configuration which is reachable via URI
     * WHEN: authentication flow is called with a not configured IdP and valid schoolId user
     * attribute
     * THEN: context status is "failed"
     */
    @ParameterizedTest
    @CsvSource({LoginActionsService.FIRST_BROKER_LOGIN_PATH, LoginActionsService.POST_BROKER_LOGIN_PATH})
    void should_deny_access_because_of_not_configured_IdP(String flowPath)
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234"), IDP_INVALID, flowPath);
        authenticator.authenticate(context);
        assertEquals(false, context.getSuccess());
    }

    @ParameterizedTest
    @CsvSource({IDP_VALID + ",true", IDP_INVALID + ",false"})
    void should_allow_access_because_of_valid_user_idp_attribute(String idp, boolean expected){
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234"), idp, LoginActionsService.AUTHENTICATE_PATH);
        authenticator.authenticate(context);
        assertEquals(expected, context.getSuccess());
    }

    private TestAuthenticationFlowContext mockContext(String clientId, List<String> usersSchoolIds)
    {
        return mockContext(clientId, usersSchoolIds, IDP_VALID, LoginActionsService.FIRST_BROKER_LOGIN_PATH);
    }

    private TestAuthenticationFlowContext mockContext(String clientId, List<String> usersSchoolIds, String brokeredIdp, String flowPath)
    {
        TestAuthenticationFlowContext context = mock(TestAuthenticationFlowContext.class);
        // success/failure
        doCallRealMethod().when(context).success();
        doCallRealMethod().when(context).failure(any(), any());
        doCallRealMethod().when(context).forceChallenge(any());
        doCallRealMethod().when(context).attempted();
        when(context.getSuccess()).thenCallRealMethod();
        when(context.getFlowPath()).thenReturn(flowPath);
        //mock realm
        RealmModel realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn("test");
        when(realm.isRegistrationEmailAsUsername()).thenReturn(false);
        when(context.getRealm()).thenReturn(realm);
        //mock user attributes
        UserModel userModel = mock(UserModel.class);
        when(context.getUser()).thenReturn(userModel);
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM_DEFAULT, usersSchoolIds);
        if(LoginActionsService.AUTHENTICATE_PATH.equals(flowPath)){
            attributes.put(SchoolWhitelistAuthenticator.IDP_ALIAS, List.of(brokeredIdp));
            var keycloakSession = mock(KeycloakSession.class);
            when(context.getSession()).thenReturn(keycloakSession);
            UserProvider userProvider = mock(UserProvider.class);
            when(keycloakSession.users()).thenReturn(userProvider);
            FederatedIdentityModel fim = mock(FederatedIdentityModel.class);
            when(userProvider.getFederatedIdentitiesStream(any(), any())).thenReturn(Stream.of(fim));
            when(fim.getIdentityProvider()).thenReturn(brokeredIdp);
        }
        when(userModel.getAttributes()).thenReturn(attributes);
        when(userModel.getId()).thenReturn("0983762");
        //mock clientId
        AuthenticationSessionModel model = mock(AuthenticationSessionModel.class);
        when(context.getAuthenticationSession()).thenReturn(model);
        ClientModel clientModel = mock(ClientModel.class);
        when(model.getClient()).thenReturn(clientModel);
        when(model.getRealm()).thenReturn(realm);
        when(clientModel.getClientId()).thenReturn(clientId);
        if (LoginActionsService.FIRST_BROKER_LOGIN_PATH.equals(flowPath)) {
            when(model.getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE)).thenReturn(String.format("{\n"
                    + "    \"id\": \"G-8d24f6a2-2a11-482e-89c5-f5dbe329387e\",\n"
                    + "    \"brokerUsername\": \"G-8d24f6a2-2a11-482e-89c5-f5dbe329387e\",\n"
                    + "    \"brokerSessionId\": \"saml.37ffe451-9e1f-407d-b4a1-d7e30fc6a5e4::238082bb-f294-4e27-ae2b-c99f36ab210b\",\n"
                    + "    \"brokerUserId\": \"saml.G-8d24f6a2-2a11-482e-89c5-f5dbe329387e\",\n"
                    + "    \"identityProviderId\": \"%s\"\n"
                    + "}", brokeredIdp));
        }
        else if(LoginActionsService.POST_BROKER_LOGIN_PATH.equals(flowPath)){
            when(model.getAuthNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT)).thenReturn(String.format("{"
                    + "\"id\":\"2b01a832-8337-4c9d-b260-2e0b6558786b\","
                    + "\"brokerUsername\":\"idpuser\","
                    + "\"brokerSessionId\":\"keycloak-oidc.9b19439f-eaf3-4799-9c24-5f749470ca73\","
                    + "\"brokerUserId\":\"keycloak-oidc.2b01a832-8337-4c9d-b260-2e0b6558786b\","
                    + "\"email\":\"idpuser@test.de\","
                    + "\"lastName\":\"user\","
                    + "\"firstName\":\"idp\","
                    + "\"modelUsername\":\"idpuser\","
                    + "\"identityProviderId\":\"%s\""
                    + "}", brokeredIdp));
        }
        //config
        AuthenticatorConfigModel authenticatorConfigModel = mock(AuthenticatorConfigModel.class);
        when(context.getAuthenticatorConfig()).thenReturn(authenticatorConfigModel);
        Map<String, String> config = new HashMap<>();
        config.put(SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM, SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM_DEFAULT);
        config.put(SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_REALM, "test");
        config.put(SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_ID, "rest-client");
        config.put(SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_API_USER, "superSecret");
        when(authenticatorConfigModel.getConfig()).thenReturn(config);
        return context;
    }

}
