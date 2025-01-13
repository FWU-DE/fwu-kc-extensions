package de.intension.authenticator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;

import java.util.Map;

import static de.intension.authenticator.AcrDenyingAuthenticatorFactory.CONF_LOA_KEY;
import static org.keycloak.models.Constants.ACR_LOA_MAP;

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
            var loaKey = getLoaMapKey(context);
            Map<String, String> loaMap = getAcrLoaMap(client);
            if (loaMap.containsKey(loaKey)) {
                var acrValue = loaMap.get(loaKey);
                var hasAcrAttribute = context.getUser().getAttributeStream(loaKey).anyMatch(acrValue::equals);
                if (!hasAcrAttribute) {
                    logger.warnf("Denied request due to ACR in user attribute not matching client configuration. Realm %s, client %s, user %s.",
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

    private String getLoaMapKey(AuthenticationFlowContext context) {
        final String defaultKey = "mfa";
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig == null) {
            return defaultKey;
        }
        return authenticatorConfig.getConfig().getOrDefault(CONF_LOA_KEY, defaultKey);
    }

    private Map<String, String> getAcrLoaMap(ClientModel client) throws JsonProcessingException {
        TypeReference<Map<String, String>> typeRef = new TypeReference<>() {
        };
        String acrLoaMap = client.getAttribute(ACR_LOA_MAP);
        if (acrLoaMap == null) {
            logger.warnf("Client '%s' does not have attribute '%s' set.", client.getClientId(), ACR_LOA_MAP);
            return Map.of();
        }
        return mapper.readValue(acrLoaMap, typeRef);
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
