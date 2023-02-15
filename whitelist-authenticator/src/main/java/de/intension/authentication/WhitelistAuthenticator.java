package de.intension.authentication;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.utils.StringUtil;

import de.intension.authentication.rest.IdPAssignmentsClient;

/**
 * Check IdP hint against a configured whitelist.
 */
public class WhitelistAuthenticator
    implements Authenticator, IdpHintParamName
{

    private static final Logger  logger = Logger.getLogger(WhitelistAuthenticator.class);
    private final IdPAssignmentsClient client;

    public WhitelistAuthenticator(IdPAssignmentsClient client)
    {
        this.client = client;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        String providerId = getProviderIdFromContext(context);
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
        String idpHintParamName = getIdpHintParamName(context);
        String providerId = context.getUriInfo().getQueryParameters().getFirst(idpHintParamName);
        providerId = StringUtil.isNotBlank(providerId) ? providerId
                : context.getUriInfo().getQueryParameters().getFirst(AdapterConstants.KC_IDP_HINT);
        if (StringUtil.isBlank(providerId)) {
            try {
                SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext
                    .readFromAuthenticationSession(context.getAuthenticationSession(),
                                                   AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
                if (serializedCtx != null) {
                    providerId = serializedCtx.getIdentityProviderId();
                }
            } catch (Exception e) {
                logger.warn(e.getLocalizedMessage());
            }
        }
        return providerId;
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
                String apiClientSecret = getConfigEntry(context, WhitelistAuthenticatorFactory.AUTH_WHITELIST_CLIENT_SECRET, null);
                List<String> allowedIdPs = client.getListOfAllowedIdPs(clientId, apiRealm, apiClientId, apiClientSecret);
                if (allowedIdPs != null && allowedIdPs.contains(providerId)) {
                    isAllowed = true;
                }
            } catch (IOException | URISyntaxException e) {
                logger.errorf(e, "List of assigned IdPs could not fetched clientId=%s, providerId=%s, url=%s", clientId, providerId, client.getUrl());
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
        else if(defaultValue != null){
            value = defaultValue;
        } else {
            logger.errorv("Provider %s - Parameter %s must not be null", WhitelistAuthenticatorFactory.PROVIDER_ID, configKey);
        }
        return value;
    }

    public IdPAssignmentsClient getClient(){
        return client;
    }
}
