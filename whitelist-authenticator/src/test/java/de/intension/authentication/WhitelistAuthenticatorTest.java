package de.intension.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.intension.authentication.rest.IdPAssignmentsClient;
import de.intension.authentication.test.TestAuthenticationFlowContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mockito;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.MediaType;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.constants.AdapterConstants.KC_IDP_HINT;
import static org.mockito.Mockito.*;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.NOT_FOUND_404;
import static org.mockserver.model.HttpStatusCode.OK_200;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@ExtendWith(MockServerExtension.class)
@MockServerSettings(ports = {18733})
class WhitelistAuthenticatorTest {

    private static final String CLIENT_CONFIGURED     = "configured";
    private static final String CLIENT_NOT_CONFIGURED = "notConfigured";
    private static final String CLIENT_CONFIGURED_BUT_NOT_MATCH = "configuredNoMatch";
    private static final String CLIENT_CONFIGURED_GOOGLE = "configuredGoogle";

    private final ClientAndServer clientAndServer;

    public WhitelistAuthenticatorTest(ClientAndServer client){
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
        throws IOException
    {
        var context = mockContext(CLIENT_CONFIGURED, "facebook");
        authenticate(context);
        assertEquals(Boolean.TRUE, context.getSuccess());
    }

    @Test
    void should_not_whitelist_if_client_is_not_configured()
        throws IOException
    {
        var context = mockContext(CLIENT_NOT_CONFIGURED, "facebook");
        authenticate(context);
        assertEquals(Boolean.FALSE, context.getSuccess());
    }

    @Test
    void should_not_whitelist_if_idp_is_not_in_configured_list()
        throws IOException
    {
        var context = mockContext(CLIENT_CONFIGURED_BUT_NOT_MATCH, "facebook");
        authenticate(context);
        assertEquals(Boolean.FALSE, context.getSuccess());
    }

    @Test
    void should_not_whitelist_if_idp_is_not_in_list()
        throws IOException, ExecutionException, InterruptedException, TimeoutException, URISyntaxException
    {
        doGoogleIdpTest("facebook", null, Boolean.FALSE);
    }

    @Test
    void should_whitelist_if_idp_hint_is_missing()
        throws IOException, ExecutionException, InterruptedException, TimeoutException, URISyntaxException
    {
        doGoogleIdpTest("", null, Boolean.TRUE);
    }

    @Test
    void should_whitelist_if_idp_hint_is_missing_and_config_allows_missing_hint()
        throws IOException
    {
        var context = mockContext(CLIENT_CONFIGURED_GOOGLE, "");
        authenticate(context);
        assertEquals(Boolean.TRUE, context.getSuccess());
    }

    /*
    @Test
    void should_not_whitelist_if_config_allows_missing_hint_and_idp_hint_is_set()
        throws IOException, ExecutionException, InterruptedException, TimeoutException, URISyntaxException
    {
        var context = mockContext(CLIENT_CONFIGURED, "facebook");
        authenticate(context, List.of("google", ""));
        assertEquals(Boolean.FALSE, context.getSuccess());
    }*/

    @Test
    void should_whitelist_if_brokered_context_contains_valid_idp()
        throws IOException
    {
        doGoogleIdpTest(null, "google", Boolean.TRUE);
    }

    @Test
    void should_not_whitelist_because_brokered_context_contains_invalid_idp()
        throws IOException
    {
        doGoogleIdpTest(null, "facebook", Boolean.FALSE);
    }

    @Test
    void should_whitelist_because_of_valid_idp_hint_and_invalid_brokered_idp_is_ignored()
        throws IOException
    {
        doGoogleIdpTest("google", "facebook", Boolean.TRUE);
    }

    @Test
    void should_whitelist_if_idp_hint_and_brokered_context_are_missing()
        throws IOException
    {
        doGoogleIdpTest(null, null, Boolean.TRUE);
    }

    private void doGoogleIdpTest(String kcIdpHint, String brokeredIdp, Boolean expectedSuccess)
        throws IOException
    {
        var context = mockContext(CLIENT_CONFIGURED_GOOGLE, kcIdpHint, brokeredIdp);
        authenticate(context);
        assertEquals(expectedSuccess, context.getSuccess());
    }

    private TestAuthenticationFlowContext mockContext(String clientId, String kcIdpHint)
        throws JsonProcessingException
    {
        return mockContext(clientId, kcIdpHint, null);
    }

    private TestAuthenticationFlowContext mockContext(String clientId, String kcIdpHint, String brokeredIdp)
        throws JsonProcessingException
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

        if (brokeredIdp != null) {
            when(authSession.getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE)).thenReturn(String.format("{\n"
                                                                                                                       + "    \"id\": \"G-8d24f6a2-2a11-482e-89c5-f5dbe329387e\",\n"
                                                                                                                       + "    \"brokerUsername\": \"G-8d24f6a2-2a11-482e-89c5-f5dbe329387e\",\n"
                                                                                                                       + "    \"brokerSessionId\": \"saml.37ffe451-9e1f-407d-b4a1-d7e30fc6a5e4::238082bb-f294-4e27-ae2b-c99f36ab210b\",\n"
                                                                                                                       + "    \"brokerUserId\": \"saml.G-8d24f6a2-2a11-482e-89c5-f5dbe329387e\",\n"
                                                                                                                       + "    \"identityProviderId\": \"%s\"\n"
                                                                                                                       + "}", brokeredIdp));
        }
        else {
            when(authSession.getAuthNote(AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE)).thenReturn(null);
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
