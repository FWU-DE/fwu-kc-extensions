package de.intension.authentication.authenticators.license;

import java.io.IOException;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import de.intension.authentication.authenticators.rest.LicenseConnectRestClient;

public class LicenseConnectAuthenticatorFactory
    implements AuthenticatorFactory
{

    public static final String          PROVIDER_ID     = "license-connect-authenticator";
    public static final String          LICENSE_URL     = "license-url";
    public static final String          LICENSE_API_KEY = "license-api-key";

    private LicenseConnectAuthenticator licenseConnectAuthenticator;

    @Override
    public String getDisplayType()
    {
        return "License Connect Authenticator";
    }

    @Override
    public String getReferenceCategory()
    {
        return "License Connect";
    }

    @Override
    public boolean isConfigurable()
    {
        return false;
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
    };

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
        return "Fetch the licenses associated with user";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return List.of();
    }

    @Override
    public Authenticator create(KeycloakSession session)
    {
        return licenseConnectAuthenticator;
    }

    @Override
    public void init(Config.Scope config)
    {
        LicenseConnectRestClient restClient = new LicenseConnectRestClient(config.get(LICENSE_URL), config.get(LICENSE_API_KEY));
        licenseConnectAuthenticator = new LicenseConnectAuthenticator(restClient);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory)
    {
        // Nothing to implement
    }

    @Override
    public void close()
    {
        if (this.licenseConnectAuthenticator != null) {
            try {
                this.licenseConnectAuthenticator.getRestClient().close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }
}
