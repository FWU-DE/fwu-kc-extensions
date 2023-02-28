package de.intension.authentication.schools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.utils.StringUtil;

import de.intension.authentication.rest.SchoolAssignmentsClient;
import de.intension.authentication.rest.SchoolConfigDTO;

/**
 * Check users school id against service provider white list.
 */
public class SchoolWhitelistAuthenticator
    implements Authenticator
{

    private static final Logger           logger = Logger.getLogger(SchoolWhitelistAuthenticator.class);
    private final SchoolAssignmentsClient client;

    public SchoolWhitelistAuthenticator(SchoolAssignmentsClient client)
    {
        this.client = client;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        List<String> schoolIds = getSchoolIdsFromUser(context);
        if (isPermittedServiceRequest(context, clientId, schoolIds)) {
            context.success();
        }
        else {
            logger.infof("Service Provider with clientId=%s not configured for schools=%s for User-Id=%s", clientId, Arrays.toString(schoolIds.toArray()),
                         context.getUser().getId());
            context.failure(AuthenticationFlowError.IDENTITY_PROVIDER_DISABLED, createErrorPage(context));
        }
    }

    protected Response createErrorPage(AuthenticationFlowContext context)
    {
        return ErrorPage.error(context.getSession(), context.getAuthenticationSession(),
                               Response.Status.FORBIDDEN, "spNotConfigured");
    }

    /**
     * Get school ids from user attribute.
     */
    private List<String> getSchoolIdsFromUser(AuthenticationFlowContext context)
    {
        List<String> schoolIds = null;
        String schoolsAttributeName = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM, null);
        if (StringUtil.isNotBlank(schoolsAttributeName)) {
            UserModel user = context.getUser();
            if (user != null) {
                schoolIds = user.getAttributes().get(schoolsAttributeName);
            }
        }
        else {
            logger.errorf("User attribute must not be blank for provider %s", SchoolWhitelistAuthenticatorFactory.PROVIDER_ID);
        }
        if (schoolIds == null) {
            schoolIds = new ArrayList<>();
        }
        return schoolIds;
    }

    /**
     * Checks, whether the combination of Service Provider ID (clientID) and School ID is part of the
     * whitelist.
     */
    private boolean isPermittedServiceRequest(AuthenticationFlowContext context, String clientId, List<String> schoolIds)
    {
        String apiRealm = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_REALM, context.getRealm().getName());
        String apiClientId = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_ID, null);
        String apiClientSecret = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_SECRET, null);
        String identityProvider = getProviderIdFromContext(context);

        boolean permitted = false;
        try {
            SchoolConfigDTO config = client.getListOfAllowedSchools(identityProvider, clientId, apiRealm, apiClientId, apiClientSecret);
            if (config != null && config.isAllowAll()) {
                permitted = true;
            }
            else if (config != null) {
                for (String userSchoolId : schoolIds) {
                    if (config.getVidisSchoolIdentifiers().contains(userSchoolId)) {
                        permitted = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.errorf(e, "error %s", client.getUrl());
        } catch (URISyntaxException e) {
            logger.errorf("Invalid syntax for URI %s", client.getUrl());
        }
        return permitted;
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext)
    {
        //do nothing
    }

    @Override
    public boolean requiresUser()
    {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel)
    {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel)
    {
        //do nothing
    }

    @Override
    public void close()
    {
        //do nothing
    }

    /**
     * Get value from authenticator configuration by key.
     * 
     * @param context Authentication context
     * @param configKey Key to search for
     * @param defaultValue If config is not set, default key is used
     * @return Value or default
     */
    private String getConfigEntry(AuthenticationFlowContext context, String configKey, String defaultValue)
    {
        String value = null;
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        if (config.containsKey(configKey)) {
            value = config.get(configKey);
        }
        else if (defaultValue != null) {
            value = defaultValue;
        }
        else {
            logger.errorv("Provider %s - Parameter %s must not be null", SchoolWhitelistAuthenticatorFactory.PROVIDER_ID, configKey);
        }
        return value;
    }

    /**
     * Get provider id from context.
     */
    private String getProviderIdFromContext(AuthenticationFlowContext context)
    {
        String flowPath = context.getFlowPath();
        String providerId = null;
        try {
            if (LoginActionsService.FIRST_BROKER_LOGIN_PATH.equals(flowPath)) {
                SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext
                    .readFromAuthenticationSession(context.getAuthenticationSession(),
                                                   AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
                if (serializedCtx != null) {
                    providerId = serializedCtx.getIdentityProviderId();
                }
            }
            else if (LoginActionsService.POST_BROKER_LOGIN_PATH.equals(flowPath)) {
                SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext
                    .readFromAuthenticationSession(context.getAuthenticationSession(),
                                                   PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
                if (serializedCtx != null) {
                    providerId = serializedCtx.getIdentityProviderId();
                }
            }
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage());
        }
        return providerId;
    }

    public SchoolAssignmentsClient getClient()
    {
        return client;
    }
}
