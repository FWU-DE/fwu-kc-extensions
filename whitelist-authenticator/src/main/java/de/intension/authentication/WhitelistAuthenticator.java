package de.intension.authentication;

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
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intension.authentication.dto.WhitelistEntry;

/**
 * Check IdP hint against a configured whitelist.
 */
public class WhitelistAuthenticator
    implements Authenticator, IdpHintParamName
{

    private static final Logger logger = Logger.getLogger(WhitelistAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        logger.infof("authenticate - clientId=%s realm=%s", clientId, context.getRealm().getId());
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

        String flowPath = context.getFlowPath();
        logger.infof("getProviderId - fowPath=%s", flowPath);
        String providerId = null;
        try {
            if (LoginActionsService.AUTHENTICATE_PATH.equals(flowPath)) {
                providerId = context.getUriInfo().getQueryParameters().getFirst(idpHintParamName);
                providerId = StringUtil.isNotBlank(providerId) ? providerId
                        : context.getUriInfo().getQueryParameters().getFirst(AdapterConstants.KC_IDP_HINT);
                logger.infof("getProviderId - from URI = %s", providerId);
            }
            else if (LoginActionsService.FIRST_BROKER_LOGIN_PATH.equals(flowPath)) {
                SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext
                    .readFromAuthenticationSession(context.getAuthenticationSession(),
                                                   AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
                if (serializedCtx != null) {
                    providerId = serializedCtx.getIdentityProviderId();
                    logger.infof("getProviderId - from brokered context=%s", providerId);
                } else {
                    logger.info("getProviderId - brokered context is null");
                }
            }
            else if (LoginActionsService.POST_BROKER_LOGIN_PATH.equals(flowPath)) {
                SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext
                    .readFromAuthenticationSession(context.getAuthenticationSession(),
                                                   PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
                if (serializedCtx != null) {
                    providerId = serializedCtx.getIdentityProviderId();
                    logger.infof("getProviderId - from post brokered context=%s", providerId);
                } else {
                    logger.info("getProviderId - post brokered context is null");
                }
            }
        } catch (Exception e) {
            logger.warn(e.getLocalizedMessage());
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
        if (providerId == null || providerId.isEmpty()) {
            return true;
        }
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        Map<String, String> config = authenticatorConfig.getConfig();
        String allowedIdPs = config.get(WhitelistAuthenticatorFactory.LIST_OF_ALLOWED_IDP);
        if (allowedIdPs != null && !allowedIdPs.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                List<WhitelistEntry> entries = objectMapper.readValue(allowedIdPs, new TypeReference<>() {

                });
                for (WhitelistEntry entry : entries) {
                    if (clientId.equals(entry.getClientId())) {
                        return entry.getListOfIdPs().contains(providerId);
                    }
                }
            } catch (JsonProcessingException e) {
                logger.error("Invalid whitelist format for IdP configuration", e);
            }
        }
        return false;
    }
}
