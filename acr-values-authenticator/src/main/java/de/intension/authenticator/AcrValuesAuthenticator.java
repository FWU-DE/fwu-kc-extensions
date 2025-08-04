package de.intension.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.utils.AcrUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Deprecated
public class AcrValuesAuthenticator
        implements Authenticator {

    private static final Logger logger = Logger.getLogger(AcrValuesAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        ClientModel client = context.getAuthenticationSession().getClient();
        List<String> defaultAcrToLoa = AcrUtils.getDefaultAcrValues(client);
        Optional<String> firstAcrDefaultValue = defaultAcrToLoa.stream().findFirst();
        Map<String, Integer> acrLoaMapping = AcrUtils.getAcrLoaMap(client);
        // Case when default acr value is set for the client
        if (firstAcrDefaultValue.isPresent() && !firstAcrDefaultValue.get().isEmpty()) {
            context.getAuthenticationSession().setClientNote(OAuth2Constants.ACR_VALUES, firstAcrDefaultValue.get());
        }
        // Case when acr_value is set in the request
        else if (context.getAuthenticationSession().getClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION) != null) {
            String loaLevel = context.getAuthenticationSession().getClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION);
            if (acrLoaMapping != null) {
                Optional<Map.Entry<String, Integer>> matchingMapping = acrLoaMapping.entrySet().stream()
                        .filter(entry -> String.valueOf(entry.getValue()).equals(loaLevel)).findFirst();
                matchingMapping.ifPresent(entry -> context.getAuthenticationSession().setClientNote(OAuth2Constants.ACR_VALUES, entry.getKey()));
            }
        }
        // Case when we use the first mapping as the default value for acr
        else {
            Optional<String> firstMapping = acrLoaMapping.keySet().stream().findFirst();
            if (firstMapping.isPresent() && !firstMapping.get().isEmpty()) {
                context.getAuthenticationSession().setClientNote(OAuth2Constants.ACR_VALUES, firstMapping.get());
            } else {
                logger.warnf("No acr to loa mapping is specified for the client %s", client.getClientId());
            }
        }
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Nothing to do
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
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }
}