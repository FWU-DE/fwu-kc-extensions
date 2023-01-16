package de.intension.authentication.schools;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_DISPOSITION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.BinaryBody.binary;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.MediaType;

import de.intension.authentication.test.TestAuthenticationFlowContext;
import de.intension.authentication.test.TestSchoolWhitelistAuthenticator;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {18733})
class SchoolWhitelistAuthenticatorTest
{
    private final ClientAndServer clientAndServer;

    public SchoolWhitelistAuthenticatorTest(ClientAndServer client)
        throws IOException
    {
        clientAndServer = client;
        initMockServer();
    }

    @BeforeEach
    void clearCache()
    {
        WhiteListCache.getInstance().clear();
    }

    /**
     * Add expectation to mock server.
     */
    void initMockServer()
        throws IOException
    {
        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("de/intension/authentication/schools/school_whitelist.json"));
        clientAndServer
            .when(
                  request())
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withHeaders(
                                      header(CONTENT_TYPE.toString(), MediaType.JSON_UTF_8.getType()),
                                      header(CONTENT_DISPOSITION.toString(), "form-data; name=\"school_whitelist.json\"; filename=\"school_whitelist.json\""),
                                      header(CACHE_CONTROL.toString(), "must-revalidate, post-check=0, pre-check=0"))
                         .withBody(binary(pdfBytes)));
    }

    /**
     * GIVEN: valid whitelist configuration which is reachable via URI
     * WHEN: authentication flow is called with a configured clientId and valid schoolId user attribute
     * THEN: context status is "success"
     */
    @Test
    void should_permit_access_because_of_matching_clientId_and_schoolId()
        throws Exception
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234"), false);
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
        throws Exception
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234", "5678"), false);
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
        throws Exception
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234x"), false);
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
        throws Exception
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", new ArrayList<>(), false);
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
        throws Exception
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("not-configured-client", List.of("1234"), false);
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
        throws Exception
    {
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("allow-all-client", List.of("1234"), false);
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
        throws Exception
    {
        WhiteListCache.getInstance().clear();
        SchoolWhitelistAuthenticator authenticator = new TestSchoolWhitelistAuthenticator();
        TestAuthenticationFlowContext context = mockContext("test-client", List.of("1234"), true);
        authenticator.authenticate(context);
        assertEquals(false, context.getSuccess());
    }

    private TestAuthenticationFlowContext mockContext(String clientId, List<String> usersSchoolIds, boolean invalidUri)
    {
        TestAuthenticationFlowContext context = mock(TestAuthenticationFlowContext.class);
        // success/failure
        doCallRealMethod().when(context).success();
        doCallRealMethod().when(context).failure(any(), any());
        doCallRealMethod().when(context).forceChallenge(any());
        doCallRealMethod().when(context).attempted();
        when(context.getSuccess()).thenCallRealMethod();

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
        if (invalidUri) {
            config.put(SchoolWhitelistAuthenticatorFactory.WHITELIST_URI_PARAM, "http://invaliduri:18744/school_whitelist2.json");
        }
        else {
            config.put(SchoolWhitelistAuthenticatorFactory.WHITELIST_URI_PARAM, "http://localhost:18733/school_whitelist.json");
        }
        config.put(SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM, SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM_DEFAULT);
        when(authenticatorConfigModel.getConfig()).thenReturn(config);
        return context;
    }

}
