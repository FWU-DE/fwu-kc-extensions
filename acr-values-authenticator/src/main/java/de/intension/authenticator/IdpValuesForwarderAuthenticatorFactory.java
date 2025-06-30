package de.intension.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class IdpValuesForwarderAuthenticatorFactory
        implements AuthenticatorFactory {

    public static final String PARAM_NAME = "paramName";
    public static final String ACR_FORWARDING = "acrForwarding";
    public static final String PARAM_NAME_DEFAULT = "audience";

    public static final String PROVIDER_ID = "idp-value-forwarder-authenticator";

    @Override
    public String getDisplayType() {
        return "IDP values params appender";
    }

    @Override
    public String getReferenceCategory() {
        return "IDP values";
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
        return "Adds the custom parameter to request which is redirected to the IDP";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                new ProviderConfigProperty(PARAM_NAME, "Param name to forward to IDP",
                        "Param name which needs to be forwarded with request to the IDP",
                        ProviderConfigProperty.STRING_TYPE, PARAM_NAME_DEFAULT), new ProviderConfigProperty(ACR_FORWARDING, "ACR forwarding allowed?",
                        "Should ACR value be passed along in the request to IDP?",
                        ProviderConfigProperty.BOOLEAN_TYPE, false));
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new IdpValuesForwarderAuthenticator();
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
