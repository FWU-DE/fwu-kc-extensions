package de.intension.authentication.schools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.models.FederatedIdentityModel;
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

    public static final String            IDP_ALIAS = "idpAlias";
    private static final Logger           logger    = Logger.getLogger(SchoolWhitelistAuthenticator.class);
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
        String identityProvider = getProviderIdFromContext(context);
        if (isPermittedServiceRequest(context, clientId, schoolIds, identityProvider)) {
            context.success();
        }
        else {
            logger.infof("Combination of IdP=%s and Service Provider with clientId=%s not configured for schools=%s for User-Id=%s", identityProvider, clientId,
                         Arrays.toString(schoolIds.toArray()), context.getUser().getId());
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
    private boolean isPermittedServiceRequest(AuthenticationFlowContext context, String clientId, List<String> schoolIds, String identityProvider)
    {
        String apiRealm = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_REALM, context.getRealm().getName());
        String apiClientId = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_ID, null);
        String apiClientGrantType = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_GRANT_TYPE, null);
        String apiClientSecret = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_SECRET, null);
        String apiClientUser = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_API_USER, null);
        String apiClientPassword = getConfigEntry(context, SchoolWhitelistAuthenticatorFactory.AUTH_WHITELIST_API_PASSWORD, null);

        boolean permitted = false;
        try {
            SchoolConfigDTO config = client.getListOfAllowedSchools(identityProvider, clientId, apiRealm, apiClientId, apiClientGrantType, apiClientSecret,
                                                                    apiClientUser, apiClientPassword);
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
        logger.infof("getProviderId - flowPath=%s", flowPath);
        String providerId = null;
        try {
            if (LoginActionsService.AUTHENTICATE_PATH.equals(flowPath)) {
                providerId = getProviderIdFromFederatedIdentitesStream(context);
                logger.infof("getProviderId - from FederatedIdentitesStream = %s", providerId);
            }
            else if (LoginActionsService.FIRST_BROKER_LOGIN_PATH.equals(flowPath)) {
                providerId = getProviderIdFromBrokeredContextNote(context);
                logger.infof("First Broker Login: getProviderId - from brokered context = %s", providerId);
            }
            else if (LoginActionsService.POST_BROKER_LOGIN_PATH.equals(flowPath)) {
                providerId = getProviderIdFromPostBrokerLoginContext(context);
                logger.infof("Post Broker Login: getProviderId - from brokered context = %s", providerId);
            }
            if (StringUtil.isBlank(providerId)) {
                providerId = getProviderIdFromUserAttributes(context);
                logger.infof("getProviderId - from userattribute = %s", providerId);
            }
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage());
        }
        return providerId;
    }

    private String getProviderIdFromFederatedIdentitesStream(AuthenticationFlowContext context)
    {
        if (context.getSession().users() != null && context.getRealm() != null && context.getUser() != null) {
            return context.getSession().users()
                .getFederatedIdentitiesStream(context.getRealm(), context.getUser()).findFirst().map(FederatedIdentityModel::getIdentityProvider)
                .orElse(null);
        }
        return null;
    }

    private String getProviderIdFromPostBrokerLoginContext(AuthenticationFlowContext context)
    {
        return Optional.ofNullable(SerializedBrokeredIdentityContext
            .readFromAuthenticationSession(context.getAuthenticationSession(),
                                           PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT))
            .map(SerializedBrokeredIdentityContext::getIdentityProviderId).orElse(null);
    }

    private String getProviderIdFromBrokeredContextNote(AuthenticationFlowContext context)
    {

        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext
            .readFromAuthenticationSession(context.getAuthenticationSession(),
                                           AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
        if (serializedCtx != null) {
            return serializedCtx.getIdentityProviderId();
        }
        else {
            return null;
        }
    }

    private static String getProviderIdFromUserAttributes(AuthenticationFlowContext context)
    {
        return Optional.ofNullable(context.getUser()).map(user -> user.getFirstAttribute(IDP_ALIAS)).orElse(null);
    }

    public SchoolAssignmentsClient getClient()
    {
        return client;
    }
}
