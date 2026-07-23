package de.intension.protocol.oidc.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.provider.ProviderConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class HmacPairwiseSubMapperHelper {

    public static final String HASH_ALGORITHM_PROP_NAME = "pairwiseSubHashAlgorithm";
    private static final String HASH_ALGORITHM_PROP_LABEL = "Hash algorithm";
    private static final String HASH_ALGORITHM_PROP_HELP = "Mac hash algorithm used when calculating the pairwise subject identifier.";

    public static final String LOCAL_SUB_IDENTIFIER_PROP_NAME = "pairwiseLocalSubIdentifier";
    private static final String LOCAL_SUB_IDENTIFIER_PROP_LABEL = "Local sub identifier";
    private static final String LOCAL_SUB_IDENTIFIER_PROP_HELP = "Local sub identifier is used when calculating the pairwise subject identifier. The identifier should match the attribute name of the keycloak user.";

    public static final String EXTERNAL_SUB_ATTRIBUTE_PROP_NAME = "pairwiseExternalSubAttribute";
    private static final String EXTERNAL_SUB_ATTRIBUTE_PROP_LABEL = "External sub attribute";
    private static final String EXTERNAL_SUB_ATTRIBUTE_PROP_HELP = "Optional. Name of a user attribute that already holds a pseudonymized sub value, e.g. one provided by an IdP via a mapped claim. "
            + "If set and the user has a non-blank value for this attribute, that value is used as the sub instead of computing it via HMAC. Leave blank to keep the default HMAC-based behaviour.";

    private HmacPairwiseSubMapperHelper() {
    }

    public static String generateIdentifier(ProtocolMapperModel mappingModel, UserModel user) {
        return generateIdentifier(mappingModel, getLocalIdentifierValue(user, mappingModel));
    }

    public static String generateIdentifier(ProtocolMapperModel mappingModel, String localSub) {
        String saltStr = PairwiseSubMapperHelper.getSalt(mappingModel);
        if (saltStr == null) {
            throw new IllegalStateException("Salt not available on mappingModel. Please update protocol mapper");
        }
        String algorithm = HmacPairwiseSubMapper.getHashAlgorithm(mappingModel);
        var secretKeySpec = new SecretKeySpec(saltStr.getBytes(StandardCharsets.UTF_8), algorithm);
        try {
            var mac = Mac.getInstance(algorithm);
            mac.init(secretKeySpec);
            mac.update(getSectorIdentifier(mappingModel).getBytes(StandardCharsets.UTF_8));
            mac.update(localSub.getBytes(StandardCharsets.UTF_8));
            return UUID.nameUUIDFromBytes(mac.doFinal()).toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Generating sub failed", e);
        }
    }

    /**
     * Get valid sector identifier from URI.
     */
    static String getSectorIdentifier(ProtocolMapperModel mappingModel) {
        String sectorIdentifierUri = PairwiseSubMapperHelper.getSectorIdentifierUri(mappingModel);
        return PairwiseSubMapperUtils.resolveValidSectorIdentifier(sectorIdentifierUri);
    }

    /**
     * Retrieve the local sub identifier value from the user based on the
     * configuration in the
     * mapper.
     */
    static String getLocalIdentifierValue(UserModel user, ProtocolMapperModel mappingModel) {
        String localSubIdentifier = mappingModel.getConfig().get(LOCAL_SUB_IDENTIFIER_PROP_NAME);

        if ("id".equals(localSubIdentifier)) {
            return user.getId();
        }
        return user.getAttributeStream(localSubIdentifier).filter(Objects::nonNull).findFirst().orElse(null);
    }

    /**
     * Retrieve the already-pseudonymized sub value from the user attribute configured via
     * {@link #EXTERNAL_SUB_ATTRIBUTE_PROP_NAME}, if any. Returns {@code null} when no such
     * attribute is configured, or when the user has no non-blank value for it.
     */
    static String getExternalSubValue(UserModel user, ProtocolMapperModel mappingModel) {
        String externalSubAttribute = mappingModel.getConfig().get(EXTERNAL_SUB_ATTRIBUTE_PROP_NAME);
        if (externalSubAttribute == null || externalSubAttribute.isBlank()) {
            return null;
        }
        return user.getAttributeStream(externalSubAttribute)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * Creates the mapper's configuration property for the HMAC hash algorithm.
     *
     * @return Config property item
     */
    static ProviderConfigProperty createHashAlgorithmConfig() {
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
     * Creates the mapper's configuration property for the local sub identifier
     * config to use.
     *
     * @return Config property item
     */
    static ProviderConfigProperty createLocalSubIdentifierConfig() {
        var property = new ProviderConfigProperty();
        property.setName(LOCAL_SUB_IDENTIFIER_PROP_NAME);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel(LOCAL_SUB_IDENTIFIER_PROP_LABEL);
        property.setHelpText(LOCAL_SUB_IDENTIFIER_PROP_HELP);
        property.setDefaultValue("username");
        return property;
    }

    /**
     * Creates the mapper's configuration property for the external sub attribute
     * config to use.
     *
     * @return Config property item
     */
    static ProviderConfigProperty createExternalSubAttributeConfig() {
        var property = new ProviderConfigProperty();
        property.setName(EXTERNAL_SUB_ATTRIBUTE_PROP_NAME);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel(EXTERNAL_SUB_ATTRIBUTE_PROP_LABEL);
        property.setHelpText(EXTERNAL_SUB_ATTRIBUTE_PROP_HELP);
        return property;
    }
}