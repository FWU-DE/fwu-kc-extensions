package de.intension.protocol.oidc.mappers;

import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import javax.crypto.Mac;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper.*;

/**
 * Pairwise identifier mapper using
 * <a href="https://datatracker.ietf.org/doc/html/rfc2104">HMAC</a>.
 * This OIDC mapper will replace the {@code sub} field in the token with a
 * HMAC-hashed user ID
 * instead of the user ID. Sector identifier is mandatory for this mapper and
 * the file must exist
 * on the remote host, but re-direct URLs are ignored.
 *
 * @see <a href=
 * "https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#mac-algorithms">mac-algorithms</a>
 */
public class HmacPairwiseSubMapper extends AbstractOIDCProtocolMapper
        implements IPairwiseMapper {

    private static final String SECTOR_IDENTIFIER_PROP_HELP = "This is used to group different clients. Should be a valid URL where the hostname of the URL is used for hashing.";

    public static final String PAIRWISE_MISSING_SECTOR_IDENTIFIER = "pairwiseMissingSectorIdentifier";
    public static final String PROTOCOL_MAPPER_ID = "oidc-hmac-pairwise-subject-mapper";

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                    UserSessionModel userSession,
                                    ClientSessionContext clientSessionCtx) {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) {
            return token;
        }
        String localSub = HmacPairwiseSubMapperHelper.getLocalIdentifierValue(userSession.getUser(), mappingModel);
        if (localSub == null) {
            return token;
        }
        setIDTokenSubject(mappingModel, token, generateIdentifier(mappingModel, localSub));
        return token;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel,
                                            KeycloakSession session, UserSessionModel userSession,
                                            ClientSessionContext clientSessionCtx) {
        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) {
            return token;
        }
        String localSub = HmacPairwiseSubMapperHelper.getLocalIdentifierValue(userSession.getUser(), mappingModel);
        if (localSub == null) {
            return token;
        }
        setAccessTokenSubject(mappingModel, token, generateIdentifier(mappingModel, localSub));
        return token;
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel,
                                              KeycloakSession session, UserSessionModel userSession,
                                              ClientSessionContext clientSessionCtx) {
        if (!OIDCAttributeMapperHelper.includeInUserInfo(mappingModel)) {
            return token;
        }
        String localSub = HmacPairwiseSubMapperHelper.getLocalIdentifierValue(userSession.getUser(), mappingModel);
        if (localSub == null) {
            return token;
        }
        setUserInfoTokenSubject(mappingModel, token, generateIdentifier(mappingModel, localSub));
        return token;
    }

    /**
     * Set pairwise sub to {@link IDToken} object.
     *
     * @param token       Token to extend
     * @param pairwiseSub Pairwise subject identifier
     */
    protected void setIDTokenSubject(ProtocolMapperModel mapperModel, IDToken token, String pairwiseSub) {
        token.setSubject(pairwiseSub);
    }

    /**
     * Set pairwise sub to {@link AccessToken} object.
     *
     * @param token       Token to extend
     * @param pairwiseSub Pairwise subject identifier
     */
    protected void setAccessTokenSubject(ProtocolMapperModel mapperModel, AccessToken token, String pairwiseSub) {
        token.setSubject(pairwiseSub);
    }

    /**
     * Set pairwise sub to {@link IDToken} object.
     *
     * @param token       Token to extend
     * @param pairwiseSub Pairwise subject identifier
     */
    protected void setUserInfoTokenSubject(ProtocolMapperModel mapperModel, IDToken token, String pairwiseSub) {
        token.getOtherClaims().put("sub", pairwiseSub);
    }

    @Override
    public String generateIdentifier(ProtocolMapperModel mappingModel, String localSub) {
        return HmacPairwiseSubMapperHelper.generateIdentifier(mappingModel, localSub);
    }

    /**
     * Adds salt and hash algorithm to the mapper configuration properties.
     */
    @Override
    public List<ProviderConfigProperty> getAdditionalConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(PairwiseSubMapperHelper.createSaltConfig());
        configProperties.add(createHashAlgorithmConfig());
        configProperties.add(createLocalSubIdentifierConfig());
        return configProperties;
    }

    /**
     * Creates a new salt if missing and checks whether the configured hash
     * algorithm is valid.
     */
    @Override
    public void validateAdditionalConfig(KeycloakSession session, RealmModel realm,
                                         ProtocolMapperContainerModel mapperContainer,
                                         ProtocolMapperModel mapperModel)
            throws ProtocolMapperConfigException {
        validateSaltConfig(mapperModel);
        validateHashAlgorithmConfig(mapperModel);
    }


    protected static void validateSaltConfig(ProtocolMapperModel mapperModel) {
        // Generate random salt if needed
        String salt = PairwiseSubMapperHelper.getSalt(mapperModel);
        if (salt == null || salt.trim().isEmpty()) {
            salt = KeycloakModelUtils.generateId();
            PairwiseSubMapperHelper.setSalt(mapperModel, salt);
        }
    }

    protected static void validateHashAlgorithmConfig(ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        // Check that hash algorithm is set
        String algorithm = getHashAlgorithm(mapperModel);
        try {
            Mac.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ProtocolMapperConfigException("Hash algorithm '" + algorithm + "' cannot be found", e);
        }
    }

    @Override
    public String getDisplayCategory() {
        return AbstractOIDCProtocolMapper.TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType() {
        return "HMAC Pairwise subject with static sectorIdentifier";
    }

    @Override
    public String getHelpText() {
        return "Calculates a pairwise subject identifier using a salted HMAC hash and sectorIdentifier. See OpenID Connect specification for more info about pairwise subject identifiers.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        ProviderConfigProperty sectorIdentifierConfigProperty = PairwiseSubMapperHelper.createSectorIdentifierConfig();
        sectorIdentifierConfigProperty.setHelpText(SECTOR_IDENTIFIER_PROP_HELP);
        configProperties.add(sectorIdentifierConfigProperty);
        configProperties.addAll(getAdditionalConfigProperties());
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, this.getClass());
        return configProperties;
    }

    @Override
    public String getId() {
        return PROTOCOL_MAPPER_ID;
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer,
                               ProtocolMapperModel mapperModel)
            throws ProtocolMapperConfigException {
        ClientModel client = null;
        if (mapperContainer instanceof ClientModel) {
            client = (ClientModel) mapperContainer;
            validateSectorIdentifier(session, client, mapperModel);
        }
        validateAdditionalConfig(session, realm, client, mapperModel);
    }

    /**
     * Get the configured HMAC hash algorithm.
     */
    static String getHashAlgorithm(ProtocolMapperModel mappingModel) {
        return mappingModel.getConfig().get(HASH_ALGORITHM_PROP_NAME);
    }

    @Override
    public void validateSectorIdentifier(KeycloakSession session, ClientModel client, ProtocolMapperModel mapperModel)
            throws ProtocolMapperConfigException {
        String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(mapperModel);
        validateSectorIdentifierNotEmpty(sectorIdentifierUri);

        URI uri;
        try {
            // check uri format
            uri = new URI(sectorIdentifierUri);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new ProtocolMapperConfigException("Invalid Sector Identifier URI.",
                        PairwiseSubMapperValidator.PAIRWISE_MALFORMED_SECTOR_IDENTIFIER_URI);
            }
        } catch (URISyntaxException e) {
            throw new ProtocolMapperConfigException("Invalid Sector Identifier URI.",
                    PairwiseSubMapperValidator.PAIRWISE_MALFORMED_SECTOR_IDENTIFIER_URI, e);
        }
    }

    private void validateSectorIdentifierNotEmpty(String sectorIdentifierUri)
            throws ProtocolMapperConfigException {
        if (sectorIdentifierUri == null || sectorIdentifierUri.isEmpty()) {
            throw new ProtocolMapperConfigException("Sector Identifier must not be null or empty.",
                    PAIRWISE_MISSING_SECTOR_IDENTIFIER);
        }
    }
}
