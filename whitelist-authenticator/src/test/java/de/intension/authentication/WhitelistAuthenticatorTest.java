package de.intension.authentication;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.constants.AdapterConstants.KC_IDP_HINT;
import static org.mockito.Mockito.*;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.NOT_FOUND_404;
import static org.mockserver.model.HttpStatusCode.OK_200;

import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.*;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.MediaType;

import de.intension.authentication.rest.IdPAssignmentsClient;
import de.intension.authentication.test.TestAuthenticationFlowContext;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {18733})
class WhitelistAuthenticatorTest
{

    private static final String   CLIENT_CONFIGURED               = "configured";
    private static final String   CLIENT_NOT_CONFIGURED           = "notConfigured";
    private static final String   CLIENT_CONFIGURED_BUT_NOT_MATCH = "configuredNoMatch";
    private static final String   CLIENT_CONFIGURED_GOOGLE        = "configuredGoogle";

    private final ClientAndServer clientAndServer;

    public WhitelistAuthenticatorTest(ClientAndServer client)
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
                  request().withPath(String.format("/service-provider/%s/idp-assignments", CLIENT_CONFIGURED)))
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withHeaders(
                                      header(CONTENT_TYPE.toString(), MediaType.JSON_UTF_8.getType()))
                         .withBody("[\"facebook\", \"google\"]"));
        clientAndServer
            .when(
                  request().withPath(String.format("/service-provider/%s/idp-assignments", CLIENT_CONFIGURED_GOOGLE)))
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withHeaders(
                                      header(CONTENT_TYPE.toString(), MediaType.JSON_UTF_8.getType()))
                         .withBody("[\"google\"]"));
        clientAndServer
            .when(
                  request().withPath(String.format("/service-provider/%s/idp-assignments", CLIENT_CONFIGURED_BUT_NOT_MATCH)))
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withHeaders(
                                      header(CONTENT_TYPE.toString(), MediaType.JSON_UTF_8.getType()))
                         .withBody("[\"github\", \"microsoft\"]"));
        clientAndServer
            .when(
                  request().withPath(String.format("/service-provider/%s/idp-assignments", CLIENT_NOT_CONFIGURED)))
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

    @Test
    void should_whitelist()
    {
        var context = mockContext(CLIENT_CONFIGURED, "facebook");
        authenticate(context);
        assertEquals(Boolean.TRUE, context.getSuccess());
    }

    @Test
    void should_not_whitelist_if_client_is_not_configured()
    {
        var context = mockContext(CLIENT_NOT_CONFIGURED, "facebook");
        authenticate(context);
        assertEquals(Boolean.FALSE, context.getSuccess());
    }

    @Test
    void should_not_whitelist_if_idp_is_not_in_configured_list()
    {
        var context = mockContext(CLIENT_CONFIGURED_BUT_NOT_MATCH, "facebook");
        authenticate(context);
        assertEquals(Boolean.FALSE, context.getSuccess());
    }

    @Test
    void should_not_whitelist_if_idp_is_not_in_list()
    {
        doGoogleIdpTest("facebook", null, Boolean.FALSE);
    }

    @Test
    void should_whitelist_if_idp_hint_is_missing()
    {
        doGoogleIdpTest("", null, Boolean.TRUE);
    }

    @Test
    void should_whitelist_if_idp_hint_is_missing_and_config_allows_missing_hint()
    {
        var context = mockContext(CLIENT_CONFIGURED_GOOGLE, "");
        authenticate(context);
        assertEquals(Boolean.TRUE, context.getSuccess());
    }

    @Test
    void should_whitelist_if_brokered_context_contains_valid_idp()
    {
        doGoogleIdpTest(null, "google", Boolean.TRUE);
    }

    @Test
    void should_not_whitelist_because_brokered_context_contains_invalid_idp()
    {
        doGoogleIdpTest(null, "facebook", Boolean.FALSE);
    }

    @Test
    void should_not_whitelist_because_of_valid_idp_hint_is_ignored_and_brokered_idp_is_used()
    {
        doGoogleIdpTest("google", "facebook", Boolean.FALSE);
    }

    @Test
    void should_whitelist_if_idp_hint_and_brokered_context_are_missing()
    {
        doGoogleIdpTest(null, null, Boolean.TRUE);
    }

    @ParameterizedTest
    @CsvSource({"google,true", "facebook,false"})
    void should_whitelist_based_on_post_broker_context(String brokeredIdp, boolean expected)
    {
        var context = mockContext(CLIENT_CONFIGURED_GOOGLE, null, brokeredIdp, LoginActionsService.POST_BROKER_LOGIN_PATH, null);
        authenticate(context);
        assertEquals(expected, context.getSuccess());
    }

    @ParameterizedTest
    @CsvSource({"google,true", "facebook,false"})
    void should_whitelist_based_on_user_idp_attribute(String idp, boolean expected){
        UserModel userModel = mock(UserModel.class);
        when(userModel.getFirstAttribute(WhitelistAuthenticator.IDP_ALIAS)).thenReturn(idp);
        var context = mockContext(CLIENT_CONFIGURED_GOOGLE, null, null, LoginActionsService.AUTHENTICATE_PATH, userModel);
        authenticate(context);
        assertEquals(expected, context.getSuccess());
    }
    @ParameterizedTest
    @CsvSource({"google,true", "facebook,false"})
    void should_whitelist_based_on_federatedIdentity(String idp, boolean expected){
        UserModel userModel = mock(UserModel.class);
        var context = mockContext(CLIENT_CONFIGURED_GOOGLE, null, idp, LoginActionsService.AUTHENTICATE_PATH, userModel);
        authenticate(context);
        assertEquals(expected, context.getSuccess());
    }

    private void doGoogleIdpTest(String kcIdpHint, String brokeredIdp, Boolean expectedSuccess)
    {
        var context = mockContext(CLIENT_CONFIGURED_GOOGLE, kcIdpHint, brokeredIdp);
        authenticate(context);
        assertEquals(expectedSuccess, context.getSuccess());
    }

    private TestAuthenticationFlowContext mockContext(String clientId, String kcIdpHint)
    {
        return mockContext(clientId, kcIdpHint, null);
    }

    private TestAuthenticationFlowContext mockContext(String clientId, String kcIdpHint, String brokeredIdp)
    {
        String flowPath = LoginActionsService.AUTHENTICATE_PATH;
        if (brokeredIdp != null) {
            flowPath = LoginActionsService.FIRST_BROKER_LOGIN_PATH;
        }
        return mockContext(clientId, kcIdpHint, brokeredIdp, flowPath, null);
    }

    private TestAuthenticationFlowContext mockContext(String clientId, String kcIdpHint, String brokeredIdp, String flowPath, UserModel user)
    {
        var context = mock(TestAuthenticationFlowContext.class);

        // clientId return value
        var authSession = mock(AuthenticationSessionModel.class);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        var client = mock(ClientModel.class);
        when(authSession.getClient()).thenReturn(client);
        var realm = mock(RealmModel.class);
        when(context.getRealm()).thenReturn(realm);
        when(realm.isRegistrationEmailAsUsername()).thenReturn(false);
        when(realm.getName()).thenReturn("test");
        when(authSession.getRealm()).thenReturn(realm);
        when(context.getFlowPath()).thenReturn(flowPath);
        if (LoginActionsService.FIRST_BROKER_LOGIN_PATH.equals(flowPath)) {
            when(authSession.getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE)).thenReturn(String.format("{\n"
                    + "    \"id\": \"G-8d24f6a2-2a11-482e-89c5-f5dbe329387e\",\n"
                    + "    \"brokerUsername\": \"G-8d24f6a2-2a11-482e-89c5-f5dbe329387e\",\n"
                    + "    \"brokerSessionId\": \"saml.37ffe451-9e1f-407d-b4a1-d7e30fc6a5e4::238082bb-f294-4e27-ae2b-c99f36ab210b\",\n"
                    + "    \"brokerUserId\": \"saml.G-8d24f6a2-2a11-482e-89c5-f5dbe329387e\",\n"
                    + "    \"identityProviderId\": \"%s\"\n"
                    + "}", brokeredIdp));
        }
        else if (LoginActionsService.AUTHENTICATE_PATH.equals(flowPath)) {
            when(authSession.getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE)).thenReturn(null);
        }
        else if(LoginActionsService.POST_BROKER_LOGIN_PATH.equals(flowPath)){
            when(authSession.getAuthNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT)).thenReturn(String.format("{"
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

        if(user != null){
            when(context.getUser()).thenReturn(user);
        }

        when(client.getClientId()).thenReturn(clientId);

        // kc_id_hint
        var uriInfo = mock(UriInfo.class);
        when(context.getUriInfo()).thenReturn(uriInfo);
        if (kcIdpHint != null) {
            var map = new MultivaluedHashMap<>(Map.of(KC_IDP_HINT, kcIdpHint));
            when(uriInfo.getQueryParameters()).thenReturn(map);
        }
        else {
            when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        }

        // whitelist config
        var authConfig = mock(AuthenticatorConfigModel.class);
        when(context.getAuthenticatorConfig()).thenReturn(authConfig);

        // success/failure
        doCallRealMethod().when(context).success();
        doCallRealMethod().when(context).failure(Mockito.any(), Mockito.any());
        when(context.getSuccess()).thenCallRealMethod();

        // error page
        var keycloakSession = mock(KeycloakSession.class);
        when(context.getSession()).thenReturn(keycloakSession);
        if(user != null){
            UserProvider userProvider = mock(UserProvider.class);
            when(keycloakSession.users()).thenReturn(userProvider);
            FederatedIdentityModel fim = mock(FederatedIdentityModel.class);
            when(userProvider.getFederatedIdentitiesStream(any(), any())).thenReturn(Stream.of(fim));
            when(fim.getIdentityProvider()).thenReturn(brokeredIdp);
        }
        var provider = mock(LoginFormsProvider.class);
        when(keycloakSession.getProvider(LoginFormsProvider.class)).thenReturn(provider);
        when(provider.setAuthenticationSession(Mockito.any())).thenReturn(provider);
        when(provider.setError(Mockito.anyString(), Mockito.any())).thenReturn(provider);
        when(provider.createErrorPage(Mockito.any())).thenReturn(Response.status(FORBIDDEN).build());

        return context;
    }

    private void authenticate(AuthenticationFlowContext context)
    {
        IdPAssignmentsClient client = new IdPAssignmentsClient("http://localhost:18733/auth", "http://localhost:18733/service-provider/%s/idp-assignments");
        new WhitelistAuthenticator(client).authenticate(context);
    }

}
