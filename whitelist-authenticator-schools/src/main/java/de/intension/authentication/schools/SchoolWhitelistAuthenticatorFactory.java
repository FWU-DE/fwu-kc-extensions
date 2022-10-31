package de.intension.authentication.schools;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class SchoolWhitelistAuthenticatorFactory
    implements AuthenticatorFactory, AdapterConstants
{

    public static final String                 PROVIDER_ID            = "school-whitelist-authenticator";
    public static final String                 USER_ATTRIBUTE_PARAM   = "userAttributeName";
    public static final String                 USER_ATTRIBUTE_PARAM_DEFAULT = "ucsschoolSchool";
    public static final String                 WHITELIST_URI_PARAM    = "whitelistUri";
    public static final String                 CACHE_REFRESH_PARAM    = "cacheRefreshInterval";

    private final SchoolWhitelistAuthenticator whitelistAuthenticator = new SchoolWhitelistAuthenticator();

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
        return "Whitelist Authenticator for Schools";
    }

    @Override
    public String getHelpText()
    {
        return "Compares School IDs from User and Service Provider against a whitelist";
    }

    @Override
    public String getReferenceCategory()
    {
        return "Schools-client-whitelist";
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
                       new ProviderConfigProperty(USER_ATTRIBUTE_PARAM, "User attribute",
                               "User attribute which contains the school ids",
                               ProviderConfigProperty.STRING_TYPE, USER_ATTRIBUTE_PARAM_DEFAULT),
                       new ProviderConfigProperty(WHITELIST_URI_PARAM, "Whitelist URI",
                               "Hyperlink to the schools whitelist JSON-file configuration",
                               ProviderConfigProperty.STRING_TYPE, null),
                       new ProviderConfigProperty(CACHE_REFRESH_PARAM, "Cache refresh interval (minutes)",
                               "Defines the refresh interval for the internal cache in minutes",
                               ProviderConfigProperty.STRING_TYPE, 15));
    }

    @Override
    public void init(Config.Scope scope)
    {
        //do nothing
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory)
    {
        //do nothing
    }

    @Override
    public void close()
    {
        //do nothing
    }

}
