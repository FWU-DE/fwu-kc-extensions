package de.intension.authenticator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.utils.AcrUtils;

import java.util.Map;

/**
 * Authenticator for post login flow which denies if user attribute does not have attribute matching the LoA settings of current client.
 */
public class AcrDenyingAuthenticator implements Authenticator {
    private static final Logger logger = Logger.getLogger(AcrDenyingAuthenticator.class);

    private final KeycloakSession session;
    private final ObjectMapper mapper;

    public AcrDenyingAuthenticator(KeycloakSession session, ObjectMapper mapper) {
        this.session = session;
        this.mapper = mapper;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        try {
            var client = session.getContext().getClient();
            // get ACR to LOA mapping from client or realm
            Map<String, Integer> acrLoaMap = AcrUtils.getAcrLoaMap(client);
            if (!acrLoaMap.isEmpty()) {
                var hasAcrAttribute = context.getUser().getAttributeStream("acr_values").anyMatch(acrLoaMap::containsKey);
                if (!hasAcrAttribute) {
                    logger.warnf("Denied request due to acr_values in user attribute not matching client configuration. Realm %s, client %s, user %s.",
                            context.getRealm().getName(),
                            client.getClientId(),
                            context.getUser().getId());
                    context.failure(AuthenticationFlowError.ACCESS_DENIED);
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("Internal server error", e);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            return;
        }
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
