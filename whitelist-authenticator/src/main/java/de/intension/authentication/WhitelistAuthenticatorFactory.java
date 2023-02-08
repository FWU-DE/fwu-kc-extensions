package de.intension.authentication;

import java.io.IOException;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import de.intension.authentication.rest.IdPAssignmentsClient;

/**
 * Factory to create custom {@link WhitelistAuthenticator}.
 */
public class WhitelistAuthenticatorFactory
    implements AuthenticatorFactory, AdapterConstants
{

    public static final String     PROVIDER_ID                  = "whitelist-authenticator";
    private static final String    CONF_KC_AUTH_URL             = "kcAuthUrl";
    private static final String    CONF_REST_URL                = "restUrl";
    public static final String     AUTH_WHITELIST_REALM         = "authWhitelistRealm";
    public static final String     AUTH_WHITELIST_CLIENT_ID     = "authWhiteListClientId";
    public static final String     AUTH_WHITELIST_CLIENT_SECRET = "authWhiteListClientIdSecret";

    private WhitelistAuthenticator whitelistAuthenticator;

    @Override
    public Authenticator create(KeycloakSession keycloakSession)
    {
        return whitelistAuthenticator;
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType()
    {
        return "Whitelist Authenticator";
    }

    @Override
    public String getHelpText()
    {
        return "Checks selected IdP against a whitelist";
    }

    @Override
    public String getReferenceCategory()
    {
        return "IdP-whitelist";
    }

    @Override
    public boolean isConfigurable()
    {
        return true;
    }

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
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return List.of(
                       new ProviderConfigProperty(IdpHintParamName.IDP_HINT_PARAM_NAME, "IdP hint parameter name",
                               "Name of the URL query parameter which contains the allowed IdP.",
                               ProviderConfigProperty.STRING_TYPE, KC_IDP_HINT),
                       new ProviderConfigProperty(AUTH_WHITELIST_REALM, "Realm",
                               "Specifies the name of the realm that contains the configured client "
                                       + "for the REST API. If no value is specified, then the current realm is used.",
                               ProviderConfigProperty.STRING_TYPE, null),
                       new ProviderConfigProperty(AUTH_WHITELIST_CLIENT_ID, "Client ID",
                               "REST-API Client ID",
                               ProviderConfigProperty.STRING_TYPE, null),
                       new ProviderConfigProperty(AUTH_WHITELIST_CLIENT_SECRET, "Client Secret",
                               "REST-API Client Secret",
                               ProviderConfigProperty.PASSWORD, null));
    }

    @Override
    public void init(Config.Scope scope)
    {
        whitelistAuthenticator = new WhitelistAuthenticator(
                new IdPAssignmentsClient(
                        scope.get(CONF_KC_AUTH_URL),
                        scope.get(CONF_REST_URL)));
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory)
    {
        //not needed
    }

    @Override
    public void close()
    {
        if(whitelistAuthenticator != null){
            try {
                whitelistAuthenticator.getClient().close();
            } catch (IOException e) {
                //do nothing
            }
        }
    }

}
