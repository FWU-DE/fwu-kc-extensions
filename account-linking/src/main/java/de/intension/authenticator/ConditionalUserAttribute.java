package de.intension.authenticator;

import java.util.Map;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

@SuppressWarnings("java:S6548")
public class ConditionalUserAttribute
    implements ConditionalAuthenticator
{

    static final ConditionalUserAttribute SINGLETON = new ConditionalUserAttribute();

    @Override
    public boolean matchCondition(AuthenticationFlowContext context)
    {
        // Retrieve configuration
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        String attributeName = config.get(ConditionalUserAttributeFactory.CONF_ATTRIBUTE_NAME);
        boolean negateOutput = Boolean.parseBoolean(config.get(ConditionalUserAttributeFactory.CONF_NOT));

        UserModel user = context.getUser();
        if (user == null) {
            throw new AuthenticationFlowException(
                    "Cannot find user for obtaining particular user attributes. Authenticator: " + ConditionalUserAttributeFactory.PROVIDER_ID,
                    AuthenticationFlowError.UNKNOWN_USER);
        }
        boolean result = user.getAttributes().containsKey(attributeName);
        return negateOutput != result;
    }

    @Override
    public void action(AuthenticationFlowContext context)
    {
        // Not used
    }

    @Override
    public boolean requiresUser()
    {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user)
    {
        // Not used
    }

    @Override
    public void close()
    {
        // Does nothing
    }

}
