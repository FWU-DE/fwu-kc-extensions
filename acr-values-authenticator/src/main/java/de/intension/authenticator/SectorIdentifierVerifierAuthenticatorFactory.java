package de.intension.authenticator;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SectorIdentifierVerifierAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "sector-identifier-verifier";
    public static final String USER_ATTRIBUTE_NAME_CONFIG = "userAttributeName";
    public static final String USER_ATTRIBUTE_NAME_DEFAULT = "vidis_sector_identifier_uri";

    @Override
    public String getDisplayType() {
        return "Sector identifier URI verifier";
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
        return "Post-login authenticator that verifies the sector identifier URI echoed back by the IdP (stored in a user attribute) "
                + "matches the sector identifier URI originally sent to the IdP (stored in the authentication session). "
                + "Succeeds silently when no session note is present; denies access on mismatch.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return ProviderConfigurationBuilder.create()
                .property()
                .name(USER_ATTRIBUTE_NAME_CONFIG)
                .label("User attribute name")
                .helpText("Name of the user attribute that holds the sector identifier URI returned by the IdP (e.g. mapped via a protocol mapper).")
                .type(ProviderConfigProperty.STRING_TYPE)
                .defaultValue(USER_ATTRIBUTE_NAME_DEFAULT)
                .add()
                .build();
    }

    /**
     * Resolves the configured user attribute name from the flow context, falling back to the default.
     */
    public static String resolveAttributeName(AuthenticationFlowContext context) {
        Map<String, String> config = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .orElse(Collections.emptyMap());
        String name = config.get(USER_ATTRIBUTE_NAME_CONFIG);
        return (name != null && !name.isEmpty()) ? name : USER_ATTRIBUTE_NAME_DEFAULT;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new SectorIdentifierVerifierAuthenticator();
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
