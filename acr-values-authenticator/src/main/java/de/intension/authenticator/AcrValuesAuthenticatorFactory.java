package de.intension.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class AcrValuesAuthenticatorFactory
    implements AuthenticatorFactory
{

    public static final String PROVIDER_ID = "acr-value-param-authenticator";

    @Override
    public String getDisplayType()
    {
        return "Acr values params appender";
    }

    @Override
    public String getReferenceCategory()
    {
        return "ACR values";
    }

    @Override
    public boolean isConfigurable()
    {
        return false;
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
    public String getHelpText()
    {
        return "Adds the acr_values parameter to request which is redirected to the IDP";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return List.of();
    }

    @Override
    public Authenticator create(KeycloakSession session)
    {
        return new AcrValuesAuthenticator();
    }

    @Override
    public void init(Config.Scope config)
    {
        // Nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory factory)
    {
        // Nothing to do
    }

    @Override
    public void close()
    {
        // Nothing to do
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }
}
