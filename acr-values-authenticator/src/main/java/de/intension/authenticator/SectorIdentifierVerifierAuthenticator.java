package de.intension.authenticator;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.intension.authenticator.SectorIdentifierIdpValuesForwarderAuthFactory.SECTOR_IDENTIFIER_URI_NOTE;

/**
 * Post-login authenticator that verifies the sector identifier URI returned by the IdP (stored in a user
 * attribute) matches the sector identifier URI that was originally sent to the IdP (stored in the
 * authentication session note by {@link SectorIdentifierIdpValuesForwarderAuth}).
 *
 * <p>If no session note is present the authenticator succeeds silently. If the note is present but the
 * user attribute value does not match, authentication is denied - unless the IdP echoed back neither the
 * sector identifier URI nor a pseudonym (i.e. it ignored the sector identifier entirely), in which case
 * Keycloak falls back to generating its own pseudonym and this authenticator succeeds silently too.
 */
public class SectorIdentifierVerifierAuthenticator implements Authenticator {

    private static final Logger logger = Logger.getLogger(SectorIdentifierVerifierAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String sentSectorIdentifierUri = context.getAuthenticationSession().getClientNote(SECTOR_IDENTIFIER_URI_NOTE);

        if (sentSectorIdentifierUri == null || sentSectorIdentifierUri.isEmpty()) {
            logger.debugf("No '%s' note found in authentication session – skipping sector identifier URI verification.", SECTOR_IDENTIFIER_URI_NOTE);
            context.success();
            return;
        }

        String userAttributeName = SectorIdentifierVerifierAuthenticatorFactory.resolveAttributeName(context);
        List<String> attributeValues = context.getUser().getAttributeStream(userAttributeName).collect(Collectors.toList());

        if (attributeValues == null || attributeValues.isEmpty()) {
            if (noPseudonymMapped(context)) {
                logger.debugf("Neither sector identifier URI nor a pseudonym were sent back by the IdP for user '%s' - IdP ignored the sector identifier, falling back to a locally generated pseudonym.",
                        context.getUser().getId());
                context.success();
                return;
            }
            logger.warnf("Sector identifier URI verification failed: user '%s' has no value for attribute '%s'. Expected '%s'.",
                    context.getUser().getId(), userAttributeName, sentSectorIdentifierUri);
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        boolean matches = attributeValues.stream().anyMatch(sentSectorIdentifierUri::equals);
        if (!matches) {
            logger.warnf("Sector identifier URI verification failed: sent sector identifier URI '%s' does not match user attribute '%s' values %s for user '%s'.",
                    sentSectorIdentifierUri, userAttributeName, attributeValues, context.getUser().getId());
            context.failure(AuthenticationFlowError.ACCESS_DENIED);
            return;
        }

        logger.debugf("Sector identifier URI verification passed for user '%s': attribute '%s' contains '%s'.",
                context.getUser().getId(), userAttributeName, sentSectorIdentifierUri);
        context.success();
    }

    /**
     * Checks whether the client's HMAC pairwise subject mapper has no external sub attribute configured, or
     * the user has no non-blank value for it - meaning the IdP did not send back a pseudonym either, on top
     * of not echoing back the sector identifier URI.
     */
    private boolean noPseudonymMapped(AuthenticationFlowContext context) {
        ClientModel client = context.getAuthenticationSession().getClient();
        Optional<ProtocolMapperModel> hmacMapper = client.getProtocolMappersStream()
                .filter(mapper -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(mapper.getProtocolMapper()))
                .findFirst();
        if (hmacMapper.isEmpty()) {
            return true;
        }
        String externalSubAttribute = hmacMapper.get().getConfig().get(HmacPairwiseSubMapperHelper.EXTERNAL_SUB_ATTRIBUTE_PROP_NAME);
        if (externalSubAttribute == null || externalSubAttribute.isBlank()) {
            return true;
        }
        return context.getUser().getAttributeStream(externalSubAttribute)
                .noneMatch(value -> value != null && !value.isBlank());
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
