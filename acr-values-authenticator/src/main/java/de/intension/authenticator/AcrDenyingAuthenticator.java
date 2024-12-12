package de.intension.authenticator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;

import static org.keycloak.models.Constants.ACR_LOA_MAP;

/**
 * Authenticator for post login flow which denies if user attribute does not have attribute matching the LoA settings of current client.
 */
public class AcrDenyingAuthenticator implements Authenticator {
    private static final String ACR = "acr";
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
            Map<String, String> loaMap = getAcrLoaMap(client);
            if (loaMap.containsKey(ACR)) {
                var acrValue = loaMap.get(ACR);
                var acrParam = session.getContext().getHttpRequest().getUri().getPathParameters().get(ACR);
                if (!acrParam.contains(acrValue)) {
                    logger.warnf("Denied request due to ACR in IdP redirect URL not matching client configuration. Realm %s, client %s, user %s.",
                            context.getRealm().getName(),
                            client.getClientId(),
                            context.getUser() != null ? context.getUser().getId() : "unknown");
                    context.failure(AuthenticationFlowError.ACCESS_DENIED);
                }
            }
        } catch (Exception e) {
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
        context.success();
    }

    private Map<String, String> getAcrLoaMap(ClientModel client) throws JsonProcessingException {
        TypeReference<Map<String, String>> typeRef = new TypeReference<>() {
        };
        return mapper.readValue(client.getAttribute(ACR_LOA_MAP), typeRef);
    }

    @Override
    public void action(AuthenticationFlowContext context) {

    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
