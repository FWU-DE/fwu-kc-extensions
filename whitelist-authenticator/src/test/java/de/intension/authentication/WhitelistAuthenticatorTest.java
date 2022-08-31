package de.intension.authentication;

import static de.intension.authentication.WhitelistAuthenticatorFactory.LIST_OF_ALLOWED_IDP;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.constants.AdapterConstants.KC_IDP_HINT;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intension.authentication.dto.WhitelistEntry;

class WhitelistAuthenticatorTest
{

    @Test
    void should_whitelist()
        throws JsonProcessingException
    {
        var whitelist = new WhitelistEntry();
        whitelist.setClientId("app");
        whitelist.setListOfIdPs(List.of("facebook", "google"));
        var context = mockContext("app", "facebook", List.of(whitelist));

        authenticate(context);

        assertEquals(Boolean.TRUE, context.getSuccess());
    }

    @Test
    void should_not_whitelist_if_client_is_not_configured()
        throws JsonProcessingException
    {
        var whitelist = new WhitelistEntry();
        whitelist.setClientId("not");
        whitelist.setListOfIdPs(List.of("facebook", "google"));
        var context = mockContext("app", "facebook", List.of(whitelist));

        authenticate(context);

        assertEquals(Boolean.FALSE, context.getSuccess());
    }

    @Test
    void should_not_whitelist_if_idp_is_not_in_list()
        throws JsonProcessingException
    {
        doGoogleIdpTest("facebook", null, Boolean.FALSE);
    }

    @Test
    void should_whitelist_if_idp_hint_is_missing()
        throws JsonProcessingException
    {
        doGoogleIdpTest("", null, Boolean.TRUE);
    }

    @Test
    void should_whitelist_if_idp_hint_is_missing_and_config_allows_missing_hint()
        throws JsonProcessingException
    {
        var whitelist = new WhitelistEntry();
        whitelist.setClientId("app");
        whitelist.setListOfIdPs(List.of("google", ""));
        var context = mockContext("app", "", List.of(whitelist));

        authenticate(context);

        assertEquals(Boolean.TRUE, context.getSuccess());
    }

    @Test
    void should_not_whitelist_if_config_allows_missing_hint_and_idp_hint_is_set()
        throws JsonProcessingException
    {
        var whitelist = new WhitelistEntry();
        whitelist.setClientId("app");
        whitelist.setListOfIdPs(List.of("google", ""));
        var context = mockContext("app", "facebook", List.of(whitelist));

        authenticate(context);

        assertEquals(Boolean.FALSE, context.getSuccess());
    }

    @Test
    void should_whitelist_if_brokered_context_contains_valid_idp()
        throws JsonProcessingException
    {
        doGoogleIdpTest(null, "google", Boolean.TRUE);
    }

    @Test
    void should_not_whitelist_because_brokered_context_contains_invalid_idp()
        throws JsonProcessingException
    {
        doGoogleIdpTest(null, "facebook", Boolean.FALSE);
    }

    @Test
    void should_whitelist_because_of_valid_idp_hint_and_invalid_brokered_idp_is_ignored()
        throws JsonProcessingException
    {
        doGoogleIdpTest("google", "facebook", Boolean.TRUE);
    }

    @Test
    void should_whitelist_if_idp_hint_and_brokered_context_are_missing()
        throws JsonProcessingException
    {
        doGoogleIdpTest(null, null, Boolean.TRUE);
    }

    private void doGoogleIdpTest(String kcIdpHint, String brokeredIdp, Boolean expectedSuccess)
        throws JsonProcessingException
    {
        var whitelist = new WhitelistEntry();
        whitelist.setClientId("app");
        whitelist.setListOfIdPs(List.of("google"));
        var context = mockContext("app", kcIdpHint, List.of(whitelist), brokeredIdp);

        authenticate(context);

        assertEquals(expectedSuccess, context.getSuccess());
    }

    private TestContext mockContext(String clientId, String kcIdpHint, List<WhitelistEntry> allowedIdps)
        throws JsonProcessingException
    {
        return mockContext(clientId, kcIdpHint, allowedIdps, null);
    }

    private TestContext mockContext(String clientId, String kcIdpHint, List<WhitelistEntry> allowedIdps, String brokeredIdp)
        throws JsonProcessingException
    {
        TestContext context = mock(TestContext.class);

        // clientId return value
        var authSession = mock(AuthenticationSessionModel.class);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        var client = mock(ClientModel.class);
        when(authSession.getClient()).thenReturn(client);
        var realm = mock(RealmModel.class);
        when(authSession.getRealm()).thenReturn(realm);
        when(realm.isRegistrationEmailAsUsername()).thenReturn(false);
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
        var objectMapper = new ObjectMapper();
        var configMap = Map.of(WhitelistConstants.IDP_HINT_PARAM_NAME, AdapterConstants.KC_IDP_HINT,
                               LIST_OF_ALLOWED_IDP, objectMapper.writeValueAsString(allowedIdps));
        when(authConfig.getConfig()).thenReturn(configMap);

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
        new WhitelistAuthenticator().authenticate(context);
    }

    private abstract class TestContext
        implements AuthenticationFlowContext
    {

        private Boolean success = null;

        @Override
        public void success()
        {
            success = Boolean.TRUE;
        }

        @Override
        public void failure(AuthenticationFlowError error, Response response)
        {
            success = Boolean.FALSE;
        }

        public Boolean getSuccess()
        {
            return success;
        }
    }
}
