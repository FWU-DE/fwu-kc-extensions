package de.intension.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class NonStandardIdpValuesForwarderAuthFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "non-standard-idp-value-forwarder";
    public static final String ORIGIN_CLIENT_PARAM_NAME = "originClientId";
    public static final String ORIGIN_CLIENT_PARAM_NAME_DEFAULT = "origin_client_id";

    @Override
    public String getDisplayType() {
        return "Non-standard IDP values params appender";
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
        return "Adds the custom non standard parameter to request which is redirected to the IDP";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                new ProviderConfigProperty(ORIGIN_CLIENT_PARAM_NAME, "Origin client ID",
                        "Param name which needs to be forwarded with request to the IDP for origin client ID",
                        ProviderConfigProperty.STRING_TYPE, ORIGIN_CLIENT_PARAM_NAME_DEFAULT));
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new NonStandardIdpValuesForwarderAuth();
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
