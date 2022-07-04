package de.intension.authentication;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Factory to create custom {@link WhitelistAuthenticator}.
 */
public class WhitelistAuthenticatorFactory
    implements AuthenticatorFactory
{

    public static final String           PROVIDER_ID            = "whitelist-authenticator";
    public static final String           LIST_OF_ALLOWED_IDP    = "listOfAllowedIdPs";
    private final WhitelistAuthenticator whitelistAuthenticator = new WhitelistAuthenticator();

    @Override
    public Authenticator create(KeycloakSession keycloakSession)
    {
        return whitelistAuthenticator;
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType()
    {
        return "Whitelist Authenticator";
    }

    @Override
    public String getHelpText()
    {
        return "Checks selected IdP against a whitelist";
    }

    @Override
    public String getReferenceCategory()
    {
        return "IdP-whitelist";
    }

    @Override
    public boolean isConfigurable()
    {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices()
    {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed()
    {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return List.of(
                       new ProviderConfigProperty(LIST_OF_ALLOWED_IDP, "Whitelist of IdPs", "Configuration of allowed IdPs for specific clients.",
                               ProviderConfigProperty.TEXT_TYPE, null));
    }

    @Override
    public void init(Config.Scope scope)
    {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory)
    {
    }

    @Override
    public void close()
    {
    }

}
