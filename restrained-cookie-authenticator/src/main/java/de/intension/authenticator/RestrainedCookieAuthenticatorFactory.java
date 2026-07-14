package de.intension.authenticator;

import java.util.List;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Factory to create custom {@link RestrainedCookieAuthenticator}.
 */
@AutoService(AuthenticatorFactory.class)
public class RestrainedCookieAuthenticatorFactory implements AuthenticatorFactory
{

    public static final String PROVIDER_ID = "restrained-cookie-authenticator";
    public static final String CONFIG_RESTRAINING_IDPS = "restrainingIdPs";

    private final RestrainedCookieAuthenticator authenticator = new RestrainedCookieAuthenticator();

    @Override
    public Authenticator create(KeycloakSession session)
    {
        return authenticator;
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType()
    {
        return "Restrained Cookie Authenticator";
    }

    @Override
    public String getHelpText()
    {
        return "Restrains authentication based on a cookie.";
    }

    @Override
    public String getReferenceCategory()
    {
        return null;
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
                       new ProviderConfigProperty(CONFIG_RESTRAINING_IDPS, "Restraining IdPs",
                               "Aliases of identity providers for which cookie-based re-authentication is restrained.",
                               ProviderConfigProperty.MULTIVALUED_STRING_TYPE, null));
    }

    @Override
    public void init(Config.Scope config)
    {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory)
    {
        // no-op
    }

    @Override
    public void close()
    {
        // no-op
    }

}
