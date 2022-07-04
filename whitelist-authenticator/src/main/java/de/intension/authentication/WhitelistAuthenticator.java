package de.intension.authentication;

import java.util.Formatter;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intension.authentication.dto.WhitelistEntry;

/**
 * Check KC_IDP_HINT against a configured whitelist.
 */
public class WhitelistAuthenticator
    implements Authenticator, AdapterConstants
{

    private static final Logger logger = Logger.getLogger(WhitelistAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        String providerId = context.getUriInfo().getQueryParameters().getFirst(AdapterConstants.KC_IDP_HINT);
        if (!isAllowedIdP(context, clientId, providerId)) {
            String info = new Formatter().format("IdP with providerId=%s is not configured for clientId=%s", providerId, clientId).toString();
            logger.info(info);
            Response response = ErrorPage.error(context.getSession(), context.getAuthenticationSession(), Response.Status.FORBIDDEN, info);
            context.failure(AuthenticationFlowError.IDENTITY_PROVIDER_DISABLED, response);
        }
        else {
            context.success();
        }
    }

    @Override
    public void action(AuthenticationFlowContext context)
    {
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
    }

    @Override
    public void close()
    {
    }

    /**
     * Check combination of clientId and providerId against configured whitelist.
     */
    private boolean isAllowedIdP(AuthenticationFlowContext context, String clientId, String providerId)
    {
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
