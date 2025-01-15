package de.intension.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.utils.AcrUtils;

import java.util.Map;

import static de.intension.authenticator.AcrConditionalAuthenticatorFactory.CONF_MATCH_ACR;

public class AcrConditionalAuthenticator implements ConditionalAuthenticator {

    private static final Logger logger = Logger.getLogger(AcrConditionalAuthenticator.class);

    private static AcrConditionalAuthenticator instance;

    public static AcrConditionalAuthenticator getInstance() {
        if (instance == null) {
            instance = new AcrConditionalAuthenticator();
        }
        return instance;
    }

    @Override
    public boolean matchCondition(AuthenticationFlowContext context) {
        var matchAcr = Boolean.parseBoolean(context.getAuthenticatorConfig().getConfig().get(CONF_MATCH_ACR));
        try {
            var session = context.getSession();
            var client = session.getContext().getClient();
            // get ACR to LOA mapping from client or realm
            Map<String, Integer> acrLoaMap = AcrUtils.getAcrLoaMap(client);
            if (!acrLoaMap.isEmpty()) {
                var hasAcrAttribute = context.getUser().getAttributeStream(OAuth2Constants.ACR_VALUES).anyMatch(acrLoaMap::containsKey);
                return hasAcrAttribute == matchAcr;
            }
        } catch (Exception e) {
            logger.error("Internal server error", e);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
        return !matchAcr;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // not used
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // not used
    }

    @Override
    public void close() {
        // not used
    }
}
