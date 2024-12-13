package de.intension.authentication.authenticators.licence;

import de.intension.authentication.authenticators.licence.LicenceConnectAuthenticator;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LicenceConnectAuthenticatorFactory
    implements AuthenticatorFactory
{

    public static final String                        PROVIDER_ID      = "licence-connect-authenticator";
    public static final String                        LICENCE_URL      = "licence-url";
    public static final String                        LICENCE_API_KEY  = "licence-api-key";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(LICENCE_URL);
        property.setLabel("Rest endpoint to fetch the licence for the user");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property
            .setHelpText("Expected value of rest endpoint to fetch user licence. Authenticator will only success if the user has licence associated with it");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(LICENCE_API_KEY);
        property.setLabel("API-KEY required to fetch licence");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("API key which needs to be passed as header for authentication during the call of fetching the user licence");
        configProperties.add(property);
    }

    private LicenceConnectAuthenticator licenceConnectAuthenticator;

    @Override
    public String getDisplayType()
    {
        return "Licence Connect Authenticator";
    }

    @Override
    public String getReferenceCategory()
    {
        return "Licence Connect";
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
        return "Fetch the licences associated with user";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session)
    {
        licenceConnectAuthenticator = new LicenceConnectAuthenticator();
        return licenceConnectAuthenticator;
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
        if (this.licenceConnectAuthenticator != null) {
            try {
                this.licenceConnectAuthenticator.getRestClient().close();
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
