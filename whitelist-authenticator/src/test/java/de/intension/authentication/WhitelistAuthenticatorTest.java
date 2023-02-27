package de.intension.authentication;

import static de.intension.authentication.WhitelistAuthenticatorFactory.LIST_OF_ALLOWED_IDP;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.constants.AdapterConstants.KC_IDP_HINT;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intension.authentication.dto.WhitelistEntry;
import de.intension.authentication.test.TestAuthenticationFlowContext;

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
    void should_not_whitelist_because_of_valid_idp_hint_is_ignored_and_brokered_idp_is_used()
        throws JsonProcessingException
    {
        doGoogleIdpTest("google", "facebook", Boolean.FALSE);
    }

    @Test
    void should_whitelist_if_idp_hint_and_brokered_context_are_missing()
        throws JsonProcessingException
    {
        doGoogleIdpTest(null, null, Boolean.TRUE);
    }

    @ParameterizedTest
    @CsvSource({"google,true", "facebook,false"})
    void should_whitelist_based_on_post_broker_context(String brokeredIdp, boolean expected)
        throws JsonProcessingException
    {
        var whitelist = new WhitelistEntry();
        whitelist.setClientId("app");
        whitelist.setListOfIdPs(List.of("google"));
        var context = mockContext("app", null, List.of(whitelist), brokeredIdp, LoginActionsService.POST_BROKER_LOGIN_PATH);
        authenticate(context);
        assertEquals(expected, context.getSuccess());
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

    private TestAuthenticationFlowContext mockContext(String clientId, String kcIdpHint, List<WhitelistEntry> allowedIdps)
        throws JsonProcessingException
    {
        return mockContext(clientId, kcIdpHint, allowedIdps, null);
    }

    private TestAuthenticationFlowContext mockContext(String clientId, String kcIdpHint, List<WhitelistEntry> allowedIdps, String brokeredIdp)
        throws JsonProcessingException
    {
        String flowPath = LoginActionsService.AUTHENTICATE_PATH;
        if (brokeredIdp != null) {
            flowPath = LoginActionsService.FIRST_BROKER_LOGIN_PATH;
        }
        return mockContext(clientId, kcIdpHint, allowedIdps, brokeredIdp, flowPath);
    }

    private TestAuthenticationFlowContext mockContext(String clientId, String kcIdpHint, List<WhitelistEntry> allowedIdps, String brokeredIdp, String flowPath)
        throws JsonProcessingException
    {
        var context = mock(TestAuthenticationFlowContext.class);
        // clientId return value
        var authSession = mock(AuthenticationSessionModel.class);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        var client = mock(ClientModel.class);
        when(authSession.getClient()).thenReturn(client);
        var realm = mock(RealmModel.class);
        when(authSession.getRealm()).thenReturn(realm);
        when(realm.isRegistrationEmailAsUsername()).thenReturn(false);
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
        else {
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
                    + "}",
                                                                                                                           brokeredIdp));
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
        var configMap = Map.of(IdpHintParamName.IDP_HINT_PARAM_NAME, AdapterConstants.KC_IDP_HINT,
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

}
