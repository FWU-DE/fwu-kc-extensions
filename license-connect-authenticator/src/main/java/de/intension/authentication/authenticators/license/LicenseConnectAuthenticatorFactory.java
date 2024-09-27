package de.intension.authentication.authenticators.license;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class LicenseConnectAuthenticatorFactory
    implements AuthenticatorFactory
{

    public static final String                        PROVIDER_ID      = "license-connect-authenticator";
    public static final String                        LICENSE_URL      = "license-url";
    public static final String                        LICENSE_API_KEY  = "license-api-key";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(LICENSE_URL);
        property.setLabel("Rest endpoint to fetch the license for the user");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property
            .setHelpText("Expected value of rest endpoint to fetch user license. Authenticator will only success if the user has license associated with it");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(LICENSE_API_KEY);
        property.setLabel("API-KEY required to fetch license");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("API key which needs to be passed as header for authentication during the call of fetching the user license");
        configProperties.add(property);
    }

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
        return true;
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
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session)
    {
        licenseConnectAuthenticator = new LicenseConnectAuthenticator();
        return licenseConnectAuthenticator;
    }

    @Override
    public void init(Config.Scope config)
    {
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
