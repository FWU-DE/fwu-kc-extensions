package de.intension.authenticator;

import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.*;

class IdPLinkConditionalAuthenticatorTest
{

    private static final String IDP_ALIAS = "idp1";

    /**
     * GIVEN Keycloak user inside the context
     * WHEN user is linked to the given idp
     * THEN authenticator must return true
     */
    @Test
    void should_return_true_if_given_idp_is_linked()
    {
        IdPLinkConditionalAuthenticator authenticator = IdPLinkConditionalAuthenticator.SINGLETON;
        FederatedIdentityModel fedModel = new FederatedIdentityModel(IDP_ALIAS, "12345", "maxmuster");
        List<FederatedIdentityModel> fedModels = List.of(fedModel);
        Assertions.assertTrue(authenticator.matchCondition(mockContext(false, fedModels)));
    }

    /**
     * GIVEN Keycloak user inside the context
     * WHEN user is linked to the given idp
     * THEN authenticator must return false because of negation
     */
    @Test
    void should_return_false_if_given_idp_is_linked_because_of_negation()
    {
        IdPLinkConditionalAuthenticator authenticator = IdPLinkConditionalAuthenticator.SINGLETON;
        FederatedIdentityModel fedModel = new FederatedIdentityModel(IDP_ALIAS, "12345", "maxmuster");
        List<FederatedIdentityModel> fedModels = List.of(fedModel);
        Assertions.assertFalse(authenticator.matchCondition(mockContext(true, fedModels)));
    }

    /**
     * GIVEN Keycloak user inside the context
     * WHEN user is not linked to the given idp
     * THEN authenticator must return false
     */
    @Test
    void should_return_false_if_given_idp_is_not_linked()
    {
        IdPLinkConditionalAuthenticator authenticator = IdPLinkConditionalAuthenticator.SINGLETON;
        FederatedIdentityModel fedModel = new FederatedIdentityModel("idp2", "12345", "maxmuster");
        List<FederatedIdentityModel> fedModels = List.of(fedModel);
        Assertions.assertFalse(authenticator.matchCondition(mockContext(false, fedModels)));
    }

    /**
     * GIVEN Keycloak user inside the context
     * WHEN user is not linked to the given idp
     * THEN authenticator must return true because of negation
     */
    @Test
    void should_return_true_if_given_idp_is_not_linked_because_of_negation()
    {
        IdPLinkConditionalAuthenticator authenticator = IdPLinkConditionalAuthenticator.SINGLETON;
        FederatedIdentityModel fedModel = new FederatedIdentityModel("idp2", "12345", "maxmuster");
        List<FederatedIdentityModel> fedModels = List.of(fedModel);
        Assertions.assertTrue(authenticator.matchCondition(mockContext(true, fedModels)));
    }

    private AuthenticationFlowContext mockContext(boolean negateOutput, List<FederatedIdentityModel> models)
    {
        AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);
        KeycloakSession session = mock(KeycloakSession.class);
        when(context.getSession()).thenReturn(session);
        UserProvider userProvider = mock(UserProvider.class);
        when(session.users()).thenReturn(userProvider);
        when(userProvider.getFederatedIdentitiesStream(any(), any())).thenReturn(models.stream());
        UserModel user = mock(UserModel.class);
        when(context.getUser()).thenReturn(user);
        //mock authenticator config
        AuthenticatorConfigModel authConfig = mock(AuthenticatorConfigModel.class);
        when(context.getAuthenticatorConfig()).thenReturn(authConfig);
        HashMap<String, String> config = new HashMap<>();
        config.put(IdPLinkConditionalAuthenticatorFactory.CONF_IDP_NAME, IDP_ALIAS);
        config.put(IdPLinkConditionalAuthenticatorFactory.CONF_NOT, Boolean.toString(negateOutput));
        when(authConfig.getConfig()).thenReturn(config);
        return context;
    }

}
