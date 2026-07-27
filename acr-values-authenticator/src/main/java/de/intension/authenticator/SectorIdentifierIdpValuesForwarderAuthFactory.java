package de.intension.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class SectorIdentifierIdpValuesForwarderAuthFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "sector-identifier-idp-value-forwarder";
    public static final String SECTOR_IDENTIFIER_PARAM_NAME = "sectorIdentifierParamName";
    public static final String SECTOR_IDENTIFIER_PARAM_NAME_DEFAULT = "sector_identifier_uri";
    public static final String SECTOR_IDENTIFIER_URI_NOTE = "sector_identifier_uri";

    @Override
    public String getDisplayType() {
        return "Sector identifier URI IDP values params appender";
    }

    @Override
    public String getReferenceCategory() {
        return "Non-standard IDP values";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

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
        return "Adds the sectorIdentifierUri configured on the client's HMAC pairwise subject mapper as a request parameter forwarded to the IDP";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(SECTOR_IDENTIFIER_PARAM_NAME)
                .label("Sector identifier URI param name")
                .helpText("Param name which needs to be forwarded with request to the IDP for the client's sector identifier URI")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(SECTOR_IDENTIFIER_PARAM_NAME_DEFAULT)
                .add()
                .build();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new SectorIdentifierIdpValuesForwarderAuth();
    }

    @Override
    public void init(Config.Scope config) {
        // Nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Nothing to do
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
