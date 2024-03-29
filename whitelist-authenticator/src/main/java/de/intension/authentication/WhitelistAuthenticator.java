package de.intension.authentication;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.utils.StringUtil;

import de.intension.authentication.rest.IdPAssignmentsClient;
import jakarta.ws.rs.core.Response;

/**
 * Check IdP hint against a configured whitelist.
 */
public class WhitelistAuthenticator
    implements Authenticator, IdpHintParamName
{

    public static final String  IDP_ALIAS = "idpAlias";
    private static final Logger        logger = Logger.getLogger(WhitelistAuthenticator.class);
    private final IdPAssignmentsClient client;

    public WhitelistAuthenticator(IdPAssignmentsClient client)
    {
        this.client = client;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        logger.infof("authenticate - clientId=%s realm=%s", clientId, context.getRealm().getId());
        String providerId = getProviderIdFromContext(context);
        if (StringUtil.isNotBlank(providerId)) {
            Optional.ofNullable(context.getUser()).ifPresent(user -> user.setSingleAttribute(IDP_ALIAS, providerId));
        }
        if (!isAllowedIdP(context, clientId, providerId)) {
            logger.infof("IdP with providerId=%s is not configured for clientId=%s", providerId, clientId);
            Response response = ErrorPage.error(context.getSession(), context.getAuthenticationSession(),
                                                Response.Status.FORBIDDEN, "idpNotConfigured", providerId, clientId);
            context.failure(AuthenticationFlowError.IDENTITY_PROVIDER_DISABLED, response);
        }
        else {
            context.success();
        }
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
                providerId = getProviderIdFromIdpHint(context);
                logger.infof("getProviderId - from URI = %s", providerId);
                if (providerId == null) {
                    providerId = getProviderIdFromFederatedIdentitesStream(context);
                }
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

    private String getProviderIdFromIdpHint(AuthenticationFlowContext context)
    {
        String idpHintParamName = getIdpHintParamName(context);
        return Optional.ofNullable(context.getUriInfo().getQueryParameters().getFirst(idpHintParamName)).filter(StringUtil::isNotBlank)
                       .orElse(context.getUriInfo().getQueryParameters().getFirst(AdapterConstants.KC_IDP_HINT));
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

    @Override
    public void action(AuthenticationFlowContext context)
    {
        // not needed
    }

    @Override
    public boolean requiresUser()
    {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user)
    {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user)
    {
        // not needed
    }

    @Override
    public void close()
    {
        // not needed
    }

    /**
     * Check combination of clientId and providerId against configured whitelist.
     */
    private boolean isAllowedIdP(AuthenticationFlowContext context, String clientId, String providerId)
    {
        boolean isAllowed = false;
        if (providerId == null || providerId.isEmpty()) {
            isAllowed = true;
        }
        else {
            try {
                String apiRealm = getConfigEntry(context, WhitelistAuthenticatorFactory.AUTH_WHITELIST_REALM, context.getRealm().getName());
                String apiClientId = getConfigEntry(context, WhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_ID, null);
                String apiClientSecret = getConfigEntry(context, WhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_SECRET, "n/a");
                List<String> allowedIdPs = client.getListOfAllowedIdPs(clientId, apiRealm, apiClientId, apiClientSecret);
                logger.tracef("Retrieve allowed IDPs using client %s, realm %s, secret %s from %s", apiClientId, apiRealm, apiClientSecret.substring(0, 3),
                              client.getUrl());
                logger.debugf("Retrieved allowed IDPs %s for sp %s from %s", allowedIdPs, providerId);
                if (allowedIdPs != null && allowedIdPs.contains(providerId)) {
                    isAllowed = true;
                }
            } catch (IOException | URISyntaxException e) {
                logger.errorf(e, "List of assigned IdPs could not be fetched clientId=%s, providerId=%s, url=%s", clientId, providerId, client.getUrl());
            }
        }
        return isAllowed;
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
            logger.errorv("Provider %s - Parameter %s must not be null", WhitelistAuthenticatorFactory.PROVIDER_ID, configKey);
        }
        return value;
    }

    public IdPAssignmentsClient getClient()
    {
        return client;
    }
}
