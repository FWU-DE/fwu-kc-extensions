package de.intension.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.utils.StringUtil;

import java.util.*;

import static de.intension.authenticator.IdpValuesForwarderAuthenticatorFactory.ACR_FORWARDING;
import static de.intension.authenticator.IdpValuesForwarderAuthenticatorFactory.PARAM_NAME;
import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;

public class IdpValuesForwarderAuthenticator
        implements Authenticator {

    private static final Logger logger = Logger.getLogger(IdpValuesForwarderAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        ClientModel client = context.getAuthenticationSession().getClient();

        Map<String, String> config = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .orElse(Collections.emptyMap());

        String configParamName = config.get(PARAM_NAME);
        if (configParamName == null || configParamName.isEmpty()) {
            logger.infof("Missing configuration parameter '%s' for client '%s'", PARAM_NAME, client.getClientId());
        }

        String noteKey = LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + configParamName;
        String paramValue = resolveAcrParam(client);

        // Set the selected ACR value as a client note
        context.getAuthenticationSession().setClientNote(noteKey, paramValue);
        logger.infof("Set ACR value '%s' to client note key '%s' for client '%s'", paramValue, noteKey, client.getClientId());

        if (Boolean.parseBoolean(config.get(ACR_FORWARDING)) && StringUtil.isNotBlank(paramValue)) {
            context.getAuthenticationSession().setClientNote(OAuth2Constants.ACR_VALUES, paramValue);
            logger.debugf("Forwarding ACR value '%s' under '%s' for client '%s'", paramValue, OAuth2Constants.ACR_VALUES, client.getClientId());
        }

        context.success();
    }

    private String resolveAcrParam(ClientModel client) {
        List<String> defaultAcrToLoa = AcrUtils.getDefaultAcrValues(client);
        Optional<String> firstAcrDefault = defaultAcrToLoa.stream().filter(Objects::nonNull).filter(loa -> !loa.isEmpty()).findFirst();

        if (firstAcrDefault.isPresent()) {
            logger.debugf("Using default ACR value '%s' for client '%s'", firstAcrDefault.get(), client.getClientId());
            return firstAcrDefault.get();
        }

        Map<String, Integer> acrLoaMapping = AcrUtils.getAcrLoaMap(client);
        Optional<String> firstMapping = acrLoaMapping.keySet().stream().filter(Objects::nonNull).filter(loa -> !loa.isEmpty()).findFirst();

        if (firstMapping.isPresent()) {
            logger.debugf("Using first ACR mapping '%s' for client '%s'", firstMapping.get(), client.getClientId());
            return firstMapping.get();
        }

        logger.debugf("No ACR value or mapping found for client '%s'. Defaulting to 'none'", client.getClientId());
        return "";
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
