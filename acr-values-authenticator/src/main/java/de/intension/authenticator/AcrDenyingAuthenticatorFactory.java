package de.intension.authenticator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.List;

public class AcrDenyingAuthenticatorFactory implements AuthenticatorFactory {

    static final String CONF_LOA_KEY = "acr.loa.mapping.key";

    @Override
    public String getDisplayType() {
        return "Acr deny";
    }

    @Override
    public String getReferenceCategory() {
        return null;
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
        return "Configure in post login flow. Denies access when user does not have attribute set with value configured in clients ACR to LoA mapping for same key. Defaults to 'mfa' as key.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property(CONF_LOA_KEY, "ACR to LoA Mapping key", "Key in client's ACR to LoA Mapping. See Client details > Advanced > Advanced Settings > ACR to LoA Mapping. Defaults to 'mfa'.", ProviderConfigProperty.STRING_TYPE, null, null)
                .build();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new AcrDenyingAuthenticator(session, new ObjectMapper());
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "acr-deny";
    }
}
