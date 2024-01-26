package de.intension.authenticator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

class UserAttributeAuthenticatorTest
{

    private static final String USERNAME           = "max.muster@tbd.de";
    private static final String IDP_LINK_ATTRIBUTE = "idp_link_attribute";

    /**
     * GIVEN Keycloak user with a configured idp link attribute
     * WHEN authenticator config is set
     * THEN input form should be skipped (auth context is success)
     */
    @Test
    void should_have_context_success_when_idp_link_attribute_already_set()
    {
        UserModel user = mock(UserModel.class);
        UserAttributeAuthenticator authenticator = new UserAttributeAuthenticator();
        AuthenticationFlowContext context = mockContext(user, true, true);
        authenticator.authenticate(context);
        verify(context, times(1)).success();
    }

    /**
     * GIVEN Keycloak user with a not configured idp link attribute
     * WHEN authenticator config is not set
     * THEN authentication context should have state failure
     */
    @Test
    void should_have_context_failure_when_config_is_missing()
    {
        UserModel user = mock(UserModel.class);
        UserAttributeAuthenticator authenticator = new UserAttributeAuthenticator();
        AuthenticationFlowContext context = mockContext(user, false, true);
        authenticator.authenticate(context);
        verify(context, times(1)).failureChallenge(any(), any());
    }

    /**
     * GIVEN Keycloak user without a configured idp link attribute
     * WHEN authenticator config is set
     * THEN input form should be shown to the user
     */
    @Test
    void should_show_input_form()
    {
        UserModel user = mock(UserModel.class);
        UserAttributeAuthenticator authenticator = new UserAttributeAuthenticator();
        AuthenticationFlowContext context = mockContext(user, true, false);
        authenticator.authenticate(context);
        verify(context, times(1)).challenge(any());
    }

    /**
     * GIVEN Keycloak user without a configured idp link attribute
     * WHEN authenticator config is set and user entered a value for the idp link attribute
     * THEN user should have a idp link attribute
     */
    @Test
    void should_have_a_idp_link_user_attribute()
    {
        UserModel user = mock(UserModel.class);
        UserAttributeAuthenticator authenticator = new UserAttributeAuthenticator();
        AuthenticationFlowContext context = mockContext(user, true, false);
        authenticator.action(context);
        verify(user, times(1)).setSingleAttribute(IDP_LINK_ATTRIBUTE, USERNAME);
        verify(context, times(1)).success();
    }

    /**
     * GIVEN Keycloak user with a not configured idp link attribute
     * WHEN authenticator config is not set
     * THEN user entered a value for idp link
     * AND authentication context should have state failure because of missing config
     */
    @Test
    void should_have_context_failure_when_config_is_missing_after_auth()
    {
        UserModel user = mock(UserModel.class);
        UserAttributeAuthenticator authenticator = new UserAttributeAuthenticator();
        AuthenticationFlowContext context = mockContext(user, false, true);
        authenticator.action(context);
        verify(context, times(1)).failureChallenge(any(), any());
        verify(user, times(0)).setSingleAttribute(IDP_LINK_ATTRIBUTE, USERNAME);
    }

    /**
     * GIVEN Keycloak user without a configured idp link attribute
     * WHEN authenticator config is set
     * THEN authentication context should have state success
     */
    @Test
    void should_have_context_success_when_user_skipped_idp_link_action()
    {
        UserModel user = mock(UserModel.class);
        UserAttributeAuthenticator authenticator = new UserAttributeAuthenticator();
        AuthenticationFlowContext context = mockContext(user, true, false, true, false);
        authenticator.action(context);
        verify(user, times(0)).setSingleAttribute(IDP_LINK_ATTRIBUTE, USERNAME);
        verify(context, times(1)).success();
    }

    /**
     * GIVEN Keycloak user without a configured idp link attribute which is empty
     * WHEN authenticator config is set
     * THEN failure should be shown to the user (auth context failure)
     */
    @Test
    void should_have_context_failure_when_user_entered_empty_linked_user()
    {
        UserModel user = mock(UserModel.class);
        UserAttributeAuthenticator authenticator = new UserAttributeAuthenticator();
        AuthenticationFlowContext context = mockContext(user, true, false, false, true);
        authenticator.action(context);
        verify(user, times(0)).setSingleAttribute(IDP_LINK_ATTRIBUTE, USERNAME);
        verify(context, times(1)).failureChallenge(any(), any());
    }

    private AuthenticationFlowContext mockContext(UserModel user, boolean hasConfig, boolean userHasAttribute)
    {
        return mockContext(user, hasConfig, userHasAttribute, false, false);
    }

    private AuthenticationFlowContext mockContext(UserModel user, boolean hasConfig, boolean userHasAttribute, boolean idpLinkSkipped, boolean emptyUsername)
    {
        AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);
        KeycloakSession session = mock(KeycloakSession.class);
        when(context.getSession()).thenReturn(session);
        LoginFormsProvider provider = mock(LoginFormsProvider.class);
        when(context.form()).thenReturn(provider);
        when(session.getProvider(any())).thenReturn(provider);
        when(provider.setAuthenticationSession(any())).thenReturn(provider);
        when(provider.setError(any())).thenReturn(provider);
        AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        HttpRequest httpRequest = mock(HttpRequest.class);
        when(context.getHttpRequest()).thenReturn(httpRequest);
        MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
        if (idpLinkSkipped) {
            formData.add("cancel", "cancelled");
        }
        else if (emptyUsername) {
            formData.add("username", "");
        }
        else {
            formData.add("username", USERNAME);
        }
        when(httpRequest.getDecodedFormParameters()).thenReturn(formData);
        //mock authenticator config
        AuthenticatorConfigModel authConfig = mock(AuthenticatorConfigModel.class);
        when(context.getAuthenticatorConfig()).thenReturn(authConfig);
        HashMap<String, String> config = new HashMap<>();
        if (hasConfig) {
            config.put(UserAttributeAuthenticatorFactory.CONF_ACCOUNT_LINK_ATTRIBUTE, IDP_LINK_ATTRIBUTE);
        }
        when(authConfig.getConfig()).thenReturn(config);
        //mock user
        if (userHasAttribute) {
            when(user.getFirstAttribute(anyString())).thenReturn(USERNAME);
        }
        else {
            when(user.getFirstAttribute(anyString())).thenReturn(null);
        }
        when(context.getUser()).thenReturn(user);
        return context;
    }

}
