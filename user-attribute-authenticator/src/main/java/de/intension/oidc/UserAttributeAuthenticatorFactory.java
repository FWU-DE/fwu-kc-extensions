package de.intension.oidc;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class UserAttributeAuthenticatorFactory
    implements AuthenticatorFactory
{

    public static final String PROVIDER_ID = "user-attribute-authenticator";

    @Override
    public String getDisplayType()
    {
        return "User Attribute Authenticator";
    }

    @Override
    public String getReferenceCategory()
    {
        return "Input field to User attribute";
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
        return "Ask via input field for a value and stores it as a user attribute";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return new ArrayList<>();
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession)
    {
        return new UserAttributeAuthenticator();
    }

    @Override
    public void init(Config.Scope scope)
    {
        //do nothing
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory)
    {
        //do nothing
    }

    @Override
    public void close()
    {
        //do nothing
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }
}
