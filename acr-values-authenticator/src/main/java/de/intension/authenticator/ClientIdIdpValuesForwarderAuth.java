package de.intension.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static de.intension.authenticator.ClientIdIdpValuesForwarderAuthFactory.ORIGIN_CLIENT_PARAM_NAME;
import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;

public class ClientIdIdpValuesForwarderAuth implements Authenticator {

    private static final Logger logger = Logger.getLogger(ClientIdIdpValuesForwarderAuth.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        ClientModel client = context.getAuthenticationSession().getClient();

        Map<String, String> config = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .orElse(Collections.emptyMap());

        String configParamName = config.get(ORIGIN_CLIENT_PARAM_NAME);
        if (configParamName == null || configParamName.isEmpty()) {
            logger.infof("Missing configuration parameter '%s' for client '%s'", ORIGIN_CLIENT_PARAM_NAME, client.getClientId());
        }

        String noteKey = LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + configParamName;
        String paramValue = client.getClientId();

        context.getAuthenticationSession().setClientNote(noteKey, paramValue);
        logger.infof("Set origin client ID '%s' to client note key '%s' for client '%s'", paramValue, noteKey, client.getClientId());
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
        return true;
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
