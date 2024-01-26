package de.intension.authentication;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticatorFactory;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

public class ConfigurableIdpHintParamIdentityProviderAuthenticatorFactory
        extends IdentityProviderAuthenticatorFactory
{

    public static final String PROVIDER_ID = "configurable-hint-idp-redirector";
    public static final String CONF_LOGIN_HINT_ATTRIBUTE = "login_hint_attribute";

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType()
    {
        return "Identity Provider Redirector w/ flexible IdP hint";
    }

    @Override
    public String getHelpText()
    {
        return "Redirects to default Identity Provider or Identity Provider specified with configurable query parameter";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return List.of(
                       new ProviderConfigProperty(DEFAULT_PROVIDER, "Default Identity Provider",
                               "To automatically redirect to an identity provider set to the alias of the identity provider",
                               ProviderConfigProperty.STRING_TYPE, null),
                       new ProviderConfigProperty(IdpHintParamName.IDP_HINT_PARAM_NAME, "IdP hint parameter name",
                               "Name of the URL query parameter which contains the allowed IdP.",
                               ProviderConfigProperty.STRING_TYPE, AdapterConstants.KC_IDP_HINT),
                       new ProviderConfigProperty(CONF_LOGIN_HINT_ATTRIBUTE, "Login hint user attribute",
                                                  "Name of the user attribute which contains the login_hint.",
                                                  ProviderConfigProperty.STRING_TYPE, null));
    }

    @Override
    public Authenticator create(KeycloakSession session)
    {
        return new ConfigurableIdpHintParamIdentityProviderAuthenticator();
    }
}
