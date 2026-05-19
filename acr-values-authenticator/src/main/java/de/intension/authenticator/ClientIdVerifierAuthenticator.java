package de.intension.authenticator;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.stream.Collectors;

import static de.intension.authenticator.ClientIdIdpValuesForwarderAuthFactory.ORIGIN_CLIENT_ID_NOTE;

/**
 * Post-login authenticator that verifies the client ID returned by the IdP (stored in a user
 * attribute) matches the client ID that was originally sent to the IdP (stored in the
 * authentication session note by {@link ClientIdIdpValuesForwarderAuth}).
 *
 * <p>If no session note is present the authenticator succeeds silently. If the note is present
 * but the user attribute value does not match, authentication is denied.
 */
public class ClientIdVerifierAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(ClientIdVerifierAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String sentClientId = context.getAuthenticationSession().getClientNote(ORIGIN_CLIENT_ID_NOTE);

        if (sentClientId == null || sentClientId.isEmpty()) {
            logger.debugf("No '%s' note found in authentication session – skipping client ID verification.", ORIGIN_CLIENT_ID_NOTE);
            context.success();
            return;
        }

        String userAttributeName = ClientIdVerifierAuthenticatorFactory.resolveAttributeName(context);
        List<String> attributeValues = context.getUser().getAttributeStream(userAttributeName).collect(Collectors.toList());

        if (attributeValues == null || attributeValues.isEmpty()) {
            logger.warnf("Client ID verification failed: user '%s' has no value for attribute '%s'. Expected '%s'.",
                    context.getUser().getId(), userAttributeName, sentClientId);
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        boolean matches = attributeValues.stream().anyMatch(sentClientId::equals);
        if (!matches) {
            logger.warnf("Client ID verification failed: sent client ID '%s' does not match user attribute '%s' values %s for user '%s'.",
                    sentClientId, userAttributeName, attributeValues, context.getUser().getId());
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        logger.debugf("Client ID verification passed for user '%s': attribute '%s' contains '%s'.",
                context.getUser().getId(), userAttributeName, sentClientId);
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Nothing to do
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
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }
}
