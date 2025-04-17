package de.intension.authentication.authenticators.licence;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

public class LicenceConnectAuthenticatorFactory
        implements AuthenticatorFactory {

    public static final String PROVIDER_ID                = "licence-connect-authenticator";
    public static final String BILO_LICENSE_CLIENTS    = "bilo-license-clients";
    public static final String GENERIC_LICENSE_CLIENTS = "license-controller-clients";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(BILO_LICENSE_CLIENTS);
        property.setLabel("Clients fetching license from bilo");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property
                .setHelpText("Clients for which user licenses will be fetched from bilo. Client names should be provided in comma separated fashion");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(GENERIC_LICENSE_CLIENTS);
        property.setLabel("Clients fetching license from generic license connect");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Clients for which user licenses will be fetched from generic license connect. Client names should be provided in comma separated fashion");
        configProperties.add(property);
    }

    @Override
    public String getDisplayType() {
        return "Licence Connect Authenticator";
    }

    @Override
    public String getReferenceCategory() {
        return "Licence Connect";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Fetch the licences associated with user";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new LicenceConnectAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
        // Nothing to implement
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to implement
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
