package de.intension.protocol.oidc.mappers;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

/**
 * Pairwise identifier mapper using
 * <a href="https://datatracker.ietf.org/doc/html/rfc2104">HMAC</a>.
 * This OIDC mapper will replace the {@code sub} field in the token with a HMAC-hashed user ID
 * instead of the user ID. Sector identifier is mandatory for this mapper and the file must exist
 * on the remote host, but re-direct URLs are ignored.
 *
 * @see <a href=
 *      "https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#mac-algorithms">mac-algorithms</a>
 */
public class HmacPairwiseSubMapper extends AbstractOIDCProtocolMapper
    implements IPairwiseSubMapper
{

    private static final String HASH_ALGORITHM_PROP_NAME           = "pairwiseSubHashAlgorithm";
    private static final String HASH_ALGORITHM_PROP_LABEL          = "Hash algorithm";
    private static final String HASH_ALGORITHM_PROP_HELP           = "Mac hash algorithm used when calculating the pairwise subject identifier.";

    private static final String LOCAL_SUB_IDENTIFIER_PROP_NAME     = "pairwiseLocalSubIdentifier";
    private static final String LOCAL_SUB_IDENTIFIER_PROP_LABEL    = "Local sub identifier";
    private static final String LOCAL_SUB_IDENTIFIER_PROP_HELP     = "Local sub identifier is used when calculating the pairwise subject identifier. The identifier should match the attribute name of the keycloak user";

    private static final String SECTOR_IDENTIFIER_PROP_HELP        = "This is used to group different clients. Should be a valid URL where the hostname of the URL is used for hashing.";

    public static final String  PAIRWISE_MISSING_SECTOR_IDENTIFIER = "pairwiseMissingSectorIdentifier";

    @Override
    public String getIdPrefix()
    {
        return "hmac";
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession,
                                    ClientSessionContext clientSessionCtx)
    {
        String localSub = getLocalSubValue(userSession.getUser(), mappingModel);
        setIDTokenSubject(token, generateSub(mappingModel, getSectorIdentifier(mappingModel),
                                             localSub));
        return token;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession,
                                            ClientSessionContext clientSessionCtx)
    {
        String localSub = getLocalSubValue(userSession.getUser(), mappingModel);
        setAccessTokenSubject(token, generateSub(mappingModel, getSectorIdentifier(mappingModel),
                                                 localSub));
        return token;
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession,
                                              ClientSessionContext clientSessionCtx)
    {
        String localSub = getLocalSubValue(userSession.getUser(), mappingModel);
        setUserInfoTokenSubject(token, generateSub(mappingModel, getSectorIdentifier(mappingModel),
                                                   localSub));
        return token;
    }

    /**
     * Set pairwise sub to {@link IDToken} object.
     * 
     * @param token Token to extend
     * @param pairwiseSub Pairwise subject identifier
     */
    protected void setIDTokenSubject(IDToken token, String pairwiseSub)
    {
        token.setSubject(pairwiseSub);
    }

    /**
     * Set pairwise sub to {@link IDToken} object.
     * 
     * @param token Token to extend
     * @param pairwiseSub Pairwise subject identifier
     */
    protected void setAccessTokenSubject(IDToken token, String pairwiseSub)
    {
        token.setSubject(pairwiseSub);
    }

    /**
     * Set pairwise sub to {@link IDToken} object.
     * 
     * @param token Token to extend
     * @param pairwiseSub Pairwise subject identifier
     */
    protected void setUserInfoTokenSubject(IDToken token, String pairwiseSub)
    {
        token.getOtherClaims().put("sub", pairwiseSub);
    }

    @Override
    public String generateSub(ProtocolMapperModel mappingModel, String sectorIdentifier, String localSub)
    {
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
    public List<ProviderConfigProperty> getAdditionalConfigProperties()
    {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(PairwiseSubMapperHelper.createSaltConfig());
        configProperties.add(createHashAlgorithmConfig());
        configProperties.add(createLocalSubIdentifierConfig());
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
    public String getDisplayCategory()
    {
        return AbstractOIDCProtocolMapper.TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType()
    {
        return "HMAC Pairwise subject with static sectorIdentifier";
    }

    @Override
    public String getHelpText()
    {
        return "Calculates a pairwise subject identifier using a salted HMAC hash and sectorIdentifier. See OpenID Connect specification for more info about pairwise subject identifiers.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        ProviderConfigProperty sectorIdentifierConfigProperty = PairwiseSubMapperHelper.createSectorIdentifierConfig();
        sectorIdentifierConfigProperty.setHelpText(SECTOR_IDENTIFIER_PROP_HELP);
        configProperties.add(sectorIdentifierConfigProperty);
        configProperties.addAll(getAdditionalConfigProperties());
        return configProperties;
    }

    @Override
    public String getId()
    {
        return "oidc-" + getIdPrefix() + "-pairwise-subject-mapper";
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer, ProtocolMapperModel mapperModel)
        throws ProtocolMapperConfigException
    {
        ClientModel client = null;
        if (mapperContainer instanceof ClientModel) {
            client = (ClientModel)mapperContainer;
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

    /**
     * Creates the mapper's configuration property for the local sub identifier config to use.
     *
     * @return Config property item
     */
    private static ProviderConfigProperty createLocalSubIdentifierConfig()
    {
        var property = new ProviderConfigProperty();
        property.setName(LOCAL_SUB_IDENTIFIER_PROP_NAME);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel(LOCAL_SUB_IDENTIFIER_PROP_LABEL);
        property.setHelpText(LOCAL_SUB_IDENTIFIER_PROP_HELP);
        property.setDefaultValue("username");
        return property;
    }

    @Override
    public void validateSectorIdentifier(KeycloakSession session, ClientModel client, ProtocolMapperModel mapperModel)
        throws ProtocolMapperConfigException
    {
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

    public void validateSectorIdentifierNotEmpty(String sectorIdentifierUri)
        throws ProtocolMapperConfigException
    {
        if (sectorIdentifierUri == null || sectorIdentifierUri.isEmpty()) {
            throw new ProtocolMapperConfigException("Sector Identifier must not be null or empty.",
                    PAIRWISE_MISSING_SECTOR_IDENTIFIER);
        }
    }

    /**
     * Get valid sector identifier from URI.
     */
    private String getSectorIdentifier(ProtocolMapperModel mappingModel)
    {
        String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(mappingModel);
        return PairwiseSubMapperUtils.resolveValidSectorIdentifier(sectorIdentifierUri);
    }

    /**
     * Retrieve the local sub identifier value from the user based on the configuration in the
     * mapper.
     * 
     * @param user
     * @param mappingModel
     * @return
     */
    private String getLocalSubValue(UserModel user, ProtocolMapperModel mappingModel)
    {
        String localSubIdentifier = mappingModel.getConfig().get(LOCAL_SUB_IDENTIFIER_PROP_NAME);

        if ("id".equals(localSubIdentifier)) {
            return user.getId();
        }
        Optional<String> localSub = user.getAttributeStream(localSubIdentifier).findFirst();
        return localSub.orElseThrow(() -> new RuntimeException("Local sub identifier is not valid"));
    }

}