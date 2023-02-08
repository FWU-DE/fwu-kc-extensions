package de.intension.authentication.schools;

import java.io.IOException;
import java.util.List;

import de.intension.authentication.rest.SchoolAssignmentsClient;
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

    public static final String                 PROVIDER_ID                  = "school-whitelist-authenticator";
    public static final String                 USER_ATTRIBUTE_PARAM         = "userAttributeName";
    public static final String                 USER_ATTRIBUTE_PARAM_DEFAULT = "ucsschoolSchool";
    private static final String                CONF_KC_AUTH_URL             = "kcAuthUrl";
    private static final String                CONF_REST_URL                = "restUrl";
    public static final String                 AUTH_WHITELIST_REALM         = "authWhitelistRealm";
    public static final String                 AUTH_WHITELIST_CLIENT_ID     = "authWhiteListClientId";
    public static final String                 AUTH_WHITELIST_CLIENT_SECRET = "authWhiteListClientIdSecret";

    private SchoolWhitelistAuthenticator whitelistAuthenticator;

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
        whitelistAuthenticator = new SchoolWhitelistAuthenticator(
            new SchoolAssignmentsClient(
                scope.get(CONF_KC_AUTH_URL),
                scope.get(CONF_REST_URL)));
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory)
    {
        //do nothing
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
