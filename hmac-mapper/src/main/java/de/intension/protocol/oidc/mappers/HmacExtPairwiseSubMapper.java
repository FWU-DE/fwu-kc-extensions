package de.intension.protocol.oidc.mappers;

import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Pairwise identifier mapper using
 * <a href="https://datatracker.ietf.org/doc/html/rfc2104">HMAC</a>.
 * This OIDC mapper will replace the {@code sub} field in the token with a HMAC-hashed user ID
 * instead of the user ID. Sector identifier is mandatory for this mapper and the file must exist
 * on the remote host, but re-direct URLs are ignored.
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#mac-algorithms">mac-algorithms</a>
 */
public class HmacExtPairwiseSubMapper extends AbstractOIDCProtocolMapper implements IPairwiseSubMapper {

    private static final String HASH_ALGORITHM_PROP_NAME  = "pairwiseSubHashAlgorithm";
    private static final String HASH_ALGORITHM_PROP_LABEL = "Hash algorithm";
    private static final String HASH_ALGORITHM_PROP_HELP  = "Mac hash algorithm used when calculating the pairwise subject identifier.";

    public static final String PAIRWISE_MISSING_SECTOR_IDENTIFIER = "pairwiseMissingSectorIdentifier";

    public static final String PROVIDER_ID = "hmac-ext";

    @Override
    public String getIdPrefix() {
        return PROVIDER_ID;
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        setIDTokenSubject(token, generateSub(mappingModel, getSectorIdentifier(clientSessionCtx.getClientSession().getClient(), mappingModel), userSession.getUser().getId()));
        return token;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        setAccessTokenSubject(token, generateSub(mappingModel, getSectorIdentifier(clientSessionCtx.getClientSession().getClient(), mappingModel), userSession.getUser().getId()));
        return token;
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        setUserInfoTokenSubject(token, generateSub(mappingModel, getSectorIdentifier(clientSessionCtx.getClientSession().getClient(), mappingModel), userSession.getUser().getId()));
        return token;
    }

    /**
     * Set pairwise sub to {@link IDToken} object.
     * @param token Token to extend
     * @param pairwiseSub Pairwise subject identifier
     */
    private void setIDTokenSubject(IDToken token, String pairwiseSub) {
        token.setSubject(pairwiseSub);
    }

    /**
     * Set pairwise sub to {@link IDToken} object.
     * @param token Token to extend
     * @param pairwiseSub Pairwise subject identifier
     */
    private void setAccessTokenSubject(IDToken token, String pairwiseSub) {
        token.setSubject(pairwiseSub);
    }

    /**
     * Set pairwise sub to {@link IDToken} object.
     * @param token Token to extend
     * @param pairwiseSub Pairwise subject identifier
     */
    private void setUserInfoTokenSubject(IDToken token, String pairwiseSub) {
        token.getOtherClaims().put("sub", pairwiseSub);
    }

    @Override
    public String generateSub(ProtocolMapperModel mappingModel, String sectorIdentifier, String localSub) {
        String saltStr = PairwiseSubMapperHelper.getSalt(mappingModel);
        if (saltStr == null) {
            throw new IllegalStateException("Salt not available on mappingModel. Please update protocol mapper");
        }
        String algorithm = getHashAlgorithm(mappingModel);
        var secretKeySpec = new SecretKeySpec(saltStr.getBytes(UTF_8), algorithm);
        try {
            var mac = Mac.getInstance(algorithm);
            mac.init(secretKeySpec);
            mac.update(sectorIdentifier.getBytes(UTF_8));
            mac.update(localSub.getBytes(UTF_8));
            return UUID.nameUUIDFromBytes(mac.doFinal()).toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Generating sub failed", e);
        }
    }

    /**
     * Adds salt and hash algorithm to the mapper configuration properties.
     */
    @Override
    public List<ProviderConfigProperty> getAdditionalConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(PairwiseSubMapperHelper.createSaltConfig());
        configProperties.add(createHashAlgorithmConfig());
        return configProperties;
    }

    /**
     * Creates a new salt if missing and checks whether the configured hash algorithm is valid.
     */
    @Override
    public void validateAdditionalConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer,
                                         ProtocolMapperModel mapperModel)
            throws ProtocolMapperConfigException
    {
        // Generate random salt if needed
        String salt = PairwiseSubMapperHelper.getSalt(mapperModel);
        if (salt == null || salt.trim().isEmpty()) {
            salt = KeycloakModelUtils.generateId();
            PairwiseSubMapperHelper.setSalt(mapperModel, salt);
        }
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
        configProperties.add(PairwiseSubMapperHelper.createSectorIdentifierConfig());
        configProperties.addAll(getAdditionalConfigProperties());
        return configProperties;
    }

    @Override
    public String getId() {
        return "oidc-" + getIdPrefix() + AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX;
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
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
    private static String getHashAlgorithm(ProtocolMapperModel mappingModel)
    {
        return mappingModel.getConfig().get(HASH_ALGORITHM_PROP_NAME);
    }

    /**
     * Creates the mapper's configuration property for the HMAC hash algorithm.
     *
     * @return Config property item
     */
    private static ProviderConfigProperty createHashAlgorithmConfig()
    {
        var property = new ProviderConfigProperty();
        property.setName(HASH_ALGORITHM_PROP_NAME);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(List.of("HmacMD5", "HmacSHA1", "HmacSHA224",
                "HmacSHA256", "HmacSHA384", "HmacSHA512",
                "HmacSHA512/224", "HmacSHA512/256", "HmacSHA3-224",
                "HmacSHA3-256", "HmacSHA3-384", "HmacSHA3-512"));
        property.setLabel(HASH_ALGORITHM_PROP_LABEL);
        property.setHelpText(HASH_ALGORITHM_PROP_HELP);
        return property;
    }

    @Override
    public void validateSectorIdentifier(KeycloakSession session, ClientModel client, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(mapperModel);
        validateSectorIdentifierNotEmpty(sectorIdentifierUri);

        URI uri;
        try {
            //check uri format
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

    public void validateSectorIdentifierNotEmpty(String sectorIdentifierUri) throws ProtocolMapperConfigException {
        if(sectorIdentifierUri == null || sectorIdentifierUri.isEmpty()){
            throw new ProtocolMapperConfigException("Sector Identifier must not be null or empty.",
                    PAIRWISE_MISSING_SECTOR_IDENTIFIER);
        }
    }

    /**
     * Get valid sector identifier from URI.
     */
    private String getSectorIdentifier(ClientModel client, ProtocolMapperModel mappingModel) {
        String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(mappingModel);
        return PairwiseSubMapperUtils.resolveValidSectorIdentifier(sectorIdentifierUri);
    }

}
