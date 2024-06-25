package de.intension.protocol.oidc.mappers;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper.createHashAlgorithmConfig;
import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper.createLocalSubIdentifierConfig;

/**
 * Pairwise identifier mapper using
 * <a href="https://datatracker.ietf.org/doc/html/rfc2104">HMAC</a>.
 * This OIDC mapper will replace the {@code sub} field in the token with a
 * HMAC-hashed user ID instead of the user ID.
 * Sector identifier is mandatory for this mapper and may be any arbitrary string (as opposed to {@link HmacPairwiseSubMapper}).
 *
 * @see <a href=
 * * "https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#mac-algorithms">mac-algorithms</a>
 * @see HmacPairwiseSubMapper
 */
public class HmacSimpleSubMapper extends HmacPairwiseSubMapper {

    public static final String SECTOR_IDENTIFIER_PROP_NAME = "sectorIdentifier";
    private static final String SECTOR_IDENTIFIER_PROP_LABEL = "Sector identifier";
    private static final String SECTOR_IDENTIFIER_PROP_HELP = "This is used to group different clients. Can be **any** *string*.";

    public static final String PROTOCOL_MAPPER_ID = "oidc-hmac-simple-pairwise-subject-mapper";

    /**
     * Generates sub identifier without salt being used.
     */
    @Override
    public String generateIdentifier(ProtocolMapperModel mappingModel, String localSub) {

        String algorithm = HmacPairwiseSubMapper.getHashAlgorithm(mappingModel);
        var secretKeySpec = new SecretKeySpec(getSectorIdentifier(mappingModel).getBytes(StandardCharsets.UTF_8), algorithm);
        try {
            var mac = Mac.getInstance(algorithm);
            mac.init(secretKeySpec);
            mac.update(localSub.getBytes(StandardCharsets.UTF_8));
            return UUID.nameUUIDFromBytes(mac.doFinal()).toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Generating sub failed", e);
        }
    }

    private String getSectorIdentifier(ProtocolMapperModel mappingModel) {
        return mappingModel.getConfig().get(SECTOR_IDENTIFIER_PROP_NAME);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(createSectorIdentifierConfig());
        configProperties.addAll(getAdditionalConfigProperties());
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, this.getClass());
        return configProperties;
    }

    private static ProviderConfigProperty createSectorIdentifierConfig() {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(SECTOR_IDENTIFIER_PROP_NAME);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel(SECTOR_IDENTIFIER_PROP_LABEL);
        property.setHelpText(SECTOR_IDENTIFIER_PROP_HELP);
        return property;
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(createHashAlgorithmConfig());
        configProperties.add(createLocalSubIdentifierConfig());
        return configProperties;
    }

    @Override
    public void validateAdditionalConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        validateHashAlgorithmConfig(mapperModel);
    }

    @Override
    public void validateSectorIdentifier(KeycloakSession session, ClientModel client, ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        String sectorIdentifier = getSectorIdentifier(mapperModel);
        if (sectorIdentifier == null || sectorIdentifier.isEmpty()) {
            throw new ProtocolMapperConfigException("Sector Identifier must not be null or empty.", PAIRWISE_MISSING_SECTOR_IDENTIFIER);
        }
    }

    @Override
    public String getDisplayType() {
        return "HMAC Pairwise subject with simple sectorIdentifier";
    }

    @Override
    public String getHelpText() {
        return "Calculates a pairwise subject identifier using an unsalted HMAC hash and sectorIdentifier. See OpenID Connect specification for more info about pairwise subject identifiers.";
    }

    @Override
    public String getId() {
        return PROTOCOL_MAPPER_ID;
    }
}
