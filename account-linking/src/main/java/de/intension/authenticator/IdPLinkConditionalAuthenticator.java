package de.intension.authenticator;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalUserAttributeValueFactory;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

@SuppressWarnings("java:S6548")
public class IdPLinkConditionalAuthenticator
    implements ConditionalAuthenticator
{

    static final IdPLinkConditionalAuthenticator SINGLETON = new IdPLinkConditionalAuthenticator();

    @Override
    public boolean matchCondition(AuthenticationFlowContext context)
    {
        // Retrieve configuration
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        String idpAlias = config.get(IdPLinkConditionalAuthenticatorFactory.CONF_IDP_NAME);
        boolean negateOutput = Boolean.parseBoolean(config.get(ConditionalUserAttributeValueFactory.CONF_NOT));

        UserModel user = context.getUser();
        if (user == null) {
            throw new AuthenticationFlowException(
                    "Cannot find user for obtaining particular user attributes. Authenticator: " + ConditionalUserAttributeFactory.PROVIDER_ID,
                    AuthenticationFlowError.UNKNOWN_USER);
        }
        Stream<FederatedIdentityModel> fiStream = context.getSession().users().getFederatedIdentitiesStream(context.getRealm(), user);
        boolean hasLinkedIdP = fiStream.anyMatch(idp -> idp.getIdentityProvider().equals(idpAlias));
        return negateOutput != hasLinkedIdP;
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext)
    {
        //do nothing
    }

    @Override
    public boolean requiresUser()
    {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel)
    {
        //do nothing
    }

    @Override
    public void close()
    {
        //do nothing
    }
}
