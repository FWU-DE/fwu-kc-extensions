package de.intension.authenticator;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static de.intension.authenticator.SectorIdentifierIdpValuesForwarderAuthFactory.SECTOR_IDENTIFIER_PARAM_NAME;
import static de.intension.authenticator.SectorIdentifierIdpValuesForwarderAuthFactory.SECTOR_IDENTIFIER_URI_NOTE;
import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;

public class SectorIdentifierIdpValuesForwarderAuth implements Authenticator {

    private static final Logger logger = Logger.getLogger(SectorIdentifierIdpValuesForwarderAuth.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        ClientModel client = context.getAuthenticationSession().getClient();

        Optional<ProtocolMapperModel> hmacMapper = client.getProtocolMappersStream()
                .filter(mapper -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(mapper.getProtocolMapper()))
                .findFirst();
        if (hmacMapper.isEmpty()) {
            logger.infof("No HMAC pairwise subject mapper configured for client '%s'", client.getClientId());
            context.success();
            return;
        }

        String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(hmacMapper.get());
        if (sectorIdentifierUri == null || sectorIdentifierUri.isEmpty()) {
            logger.infof("No sectorIdentifierUri configured on HMAC pairwise subject mapper for client '%s'", client.getClientId());
            context.success();
            return;
        }

        context.getAuthenticationSession().setClientNote(SECTOR_IDENTIFIER_URI_NOTE, sectorIdentifierUri);
        logger.infof("Stored sector identifier URI '%s' under note key '%s' for post-login verification for client '%s'",
                sectorIdentifierUri, SECTOR_IDENTIFIER_URI_NOTE, client.getClientId());

        Map<String, String> config = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .orElse(Collections.emptyMap());

        String configParamName = config.get(SECTOR_IDENTIFIER_PARAM_NAME);
        if (configParamName == null || configParamName.isEmpty()) {
            logger.infof("Missing configuration parameter '%s' for client '%s'", SECTOR_IDENTIFIER_PARAM_NAME, client.getClientId());
            context.success();
            return;
        }

        String noteKey = LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + configParamName;
        context.getAuthenticationSession().setClientNote(noteKey, sectorIdentifierUri);
        logger.infof("Set sector identifier URI '%s' to client note key '%s' for client '%s'", sectorIdentifierUri, noteKey, client.getClientId());

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
