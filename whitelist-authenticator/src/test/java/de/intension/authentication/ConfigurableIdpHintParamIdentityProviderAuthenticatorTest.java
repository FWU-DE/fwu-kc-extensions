package de.intension.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.*;
import org.keycloak.protocol.ClientData;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import de.intension.authentication.test.TestAuthenticationFlowContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.UriInfo;

@Disabled
class ConfigurableIdpHintParamIdentityProviderAuthenticatorTest
{

    private static final String VALID_IDP        = "facebook";
    private static final String EMPTY_PROVIDER   = "";
    private static final String DEFAULT_PROVIDER = "IDP";

    @ParameterizedTest
    @CsvSource({
            AdapterConstants.KC_IDP_HINT + "," + AdapterConstants.KC_IDP_HINT + "," + VALID_IDP + "," + true + "," + EMPTY_PROVIDER,
            "vidis_idp_hint" + "," + "vidis_idp_hint" + "," + VALID_IDP + "," + true + "," + EMPTY_PROVIDER,
            "vidis_idp_hint" + "," + "vidis_idp_hint" + "," + EMPTY_PROVIDER + "," + false + "," + EMPTY_PROVIDER, // false because no default provider being set
            "vidis_idp_hint" + "," + "vidis_idp_hint" + "," + EMPTY_PROVIDER + "," + true + "," + DEFAULT_PROVIDER, // true because default provider is set
            AdapterConstants.KC_IDP_HINT + "," + AdapterConstants.KC_IDP_HINT + "," + "google" + "," + false + "," + EMPTY_PROVIDER,
            AdapterConstants.KC_IDP_HINT + "," + "parameter" + "," + VALID_IDP + "," + false + "," + EMPTY_PROVIDER
    })
    void testConfigurableIdpHintParamIdentityProviderAuthenticator(String idpHintParamName, String paramInUrl, String idpName, String success,
                                                                   String defaultProvider)
        throws Exception
    {
        var context = mockContext(idpHintParamName, paramInUrl, idpName, defaultProvider);

        new ConfigurableIdpHintParamIdentityProviderAuthenticator().authenticate(context);

        assertEquals(Boolean.parseBoolean(success), context.getSuccess());
    }

    private TestAuthenticationFlowContext mockContext(String idpHintParamName, String paramInUrl, String idpName, String defaultProvider)
        throws URISyntaxException
    {
        var context = mock(TestAuthenticationFlowContext.class);

        // param name config
        var authConfig = mock(AuthenticatorConfigModel.class);
        when(context.getAuthenticatorConfig()).thenReturn(authConfig);
        Map<String, String> configMap = new HashMap<>();
        configMap.put(IdpHintParamName.IDP_HINT_PARAM_NAME, idpHintParamName);
        if (defaultProvider != null) {
            configMap.put("defaultProvider", defaultProvider);
        }
        when(authConfig.getConfig()).thenReturn(configMap);

        // identity providers
        var realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn("whitelist");
        when(context.getRealm()).thenReturn(realm);
        var idp = mock(IdentityProviderModel.class);
        when(idp.isEnabled()).thenReturn(true);
        when(idp.getAlias()).thenReturn(VALID_IDP);
        var defaultIdp = mock(IdentityProviderModel.class);
        when(defaultIdp.isEnabled()).thenReturn(true);
        when(defaultIdp.getAlias()).thenReturn(DEFAULT_PROVIDER);
        when(realm.getIdentityProvidersStream()).thenReturn(Stream.of(idp, defaultIdp));

        // success/failure
        doCallRealMethod().when(context).success();
        doCallRealMethod().when(context).failure(any(), any());
        doCallRealMethod().when(context).forceChallenge(any());
        doCallRealMethod().when(context).attempted();
        when(context.getSuccess()).thenCallRealMethod();

        // uri info for param
        var uriInfo = mock(UriInfo.class);
        when(context.getUriInfo()).thenReturn(uriInfo);
        var queryParams = new MultivaluedHashMap<String, String>();
        queryParams.put(paramInUrl, List.of(idpName == null ? "" : idpName));
        when(uriInfo.getQueryParameters()).thenReturn(queryParams);
        when(uriInfo.getBaseUri()).thenReturn(new URI("https://intension.de/"));

        // session crap
        var authSession = mock(AuthenticationSessionModel.class);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        var session = mock(KeycloakSession.class);
        when(context.getSession()).thenReturn(session);
        var sessionFactory = mock(KeycloakSessionFactory.class);
        when(session.getKeycloakSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.create()).thenReturn(session);
        var transactionManager = mock(KeycloakTransactionManager.class);
        when(session.getTransactionManager()).thenReturn(transactionManager);
        var authSessionProvider = mock(AuthenticationSessionProvider.class);
        when(session.authenticationSessions()).thenReturn(authSessionProvider);
        var rootAuthSession = mock(RootAuthenticationSessionModel.class);
        when(authSessionProvider.getRootAuthenticationSession(any(), any())).thenReturn(rootAuthSession);
        when(authSession.getParentSession()).thenReturn(rootAuthSession);
        when(rootAuthSession.getId()).thenReturn("mock");
        var client = mock(ClientModel.class);
        when(authSession.getClient()).thenReturn(client);
        when(authSession.getProtocol()).thenReturn("openid-connect");
        var loginProtocol = mock(LoginProtocol.class);
        when(session.getProvider(eq(LoginProtocol.class), eq("openid-connect"))).thenReturn(loginProtocol);


        return context;
    }
}
