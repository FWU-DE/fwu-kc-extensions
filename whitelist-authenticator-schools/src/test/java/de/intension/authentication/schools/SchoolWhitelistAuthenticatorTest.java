package de.intension.authentication.schools;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.NOT_FOUND_404;
import static org.mockserver.model.HttpStatusCode.OK_200;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
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

    private final static String   WHITELIST_DEDICATED = "{\"allowAll\": false,\"vidisSchoolIdentifiers\": [\"1234\"]}";
    private final static String   WHITELIST_ALLOW_ALL = "{\"allowAll\": true,\"vidisSchoolIdentifiers\": []}";

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
                      .withQueryStringParameter("serviceProvider", "test-client"))
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
                      .withQueryStringParameter("serviceProvider", "allow-all-client"))
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
                      .withQueryStringParameter("serviceProvider", "not-configured-client"))
            .respond(
                     response()
                         .withStatusCode(NOT_FOUND_404.code())
                         .withReasonPhrase(NOT_FOUND_404.reasonPhrase()));
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
    @Test
    void should_permit_access_because_of_matching_clientId_and_schoolId()
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234"));
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

    private TestAuthenticationFlowContext mockContext(String clientId, List<String> usersSchoolIds)
    {
        TestAuthenticationFlowContext context = mock(TestAuthenticationFlowContext.class);
        // success/failure
        doCallRealMethod().when(context).success();
        doCallRealMethod().when(context).failure(any(), any());
        doCallRealMethod().when(context).forceChallenge(any());
        doCallRealMethod().when(context).attempted();
        when(context.getSuccess()).thenCallRealMethod();
        //mock realm
        RealmModel realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn("test");
        when(context.getRealm()).thenReturn(realm);
        //mock user attributes
        UserModel userModel = mock(UserModel.class);
        when(context.getUser()).thenReturn(userModel);
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM_DEFAULT, usersSchoolIds);
        when(userModel.getAttributes()).thenReturn(attributes);
        //mock clientId
        AuthenticationSessionModel model = mock(AuthenticationSessionModel.class);
        when(context.getAuthenticationSession()).thenReturn(model);
        ClientModel clientModel = mock(ClientModel.class);
        when(model.getClient()).thenReturn(clientModel);
        when(clientModel.getClientId()).thenReturn(clientId);
        //config
        AuthenticatorConfigModel authenticatorConfigModel = mock(AuthenticatorConfigModel.class);
        when(context.getAuthenticatorConfig()).thenReturn(authenticatorConfigModel);
        Map<String, String> config = new HashMap<>();
        config.put(SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM, SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM_DEFAULT);
        config.put(SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_REALM, "test");
        config.put(SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_ID, "rest-client");
        config.put(SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_SECRET, "superSecret");
        when(authenticatorConfigModel.getConfig()).thenReturn(config);
        return context;
    }

}
