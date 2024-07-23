package de.intension.protocol.oidc.mappers;

import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.StringUtil;

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
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME;

/**
 * Pairwise identifier mapper using
 * <a href="https://datatracker.ietf.org/doc/html/rfc2104">HMAC</a>.
 * This OIDC mapper will set the dedicated claim value in the token with a HMAC-hashed user ID.
 * Sector identifier is mandatory for this mapper and may be any arbitrary string (as opposed to {@link HmacPairwiseSubMapper}).
 *
 * @see <a href=
 * * "https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#mac-algorithms">mac-algorithms</a>
 * @see HmacPairwiseSubMapper
 */
public class HmacSimpleUserAttributeMapper extends HmacPairwiseSubMapper {

    public static final String SECTOR_IDENTIFIER_PROP_NAME = "sectorIdentifier";
    private static final String SECTOR_IDENTIFIER_PROP_LABEL = "Sector identifier";
    private static final String SECTOR_IDENTIFIER_PROP_HELP = "This is used to group different clients. Can be any string.";

    public static final String PROTOCOL_MAPPER_ID = "oidc-hmac-simple-user-attribute-mapper";

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
    protected void setIDTokenSubject(ProtocolMapperModel mapperModel, IDToken token, String pairwiseSub) {
        setClaimValue(mapperModel, token, pairwiseSub);
    }

    @Override
    protected void setAccessTokenSubject(ProtocolMapperModel mapperModel, AccessToken token, String pairwiseSub) {
        setClaimValue(mapperModel, token, pairwiseSub);
    }

    @Override
    protected void setUserInfoTokenSubject(ProtocolMapperModel mapperModel, IDToken token, String pairwiseSub) {
        setClaimValue(mapperModel, token, pairwiseSub);
    }

    private void setClaimValue(ProtocolMapperModel mapperModel, IDToken token, String pairwiseSub) {
        String claimName = mapperModel.getConfig().get(TOKEN_CLAIM_NAME);
        if (StringUtil.isNotBlank(claimName)) {
            token.setOtherClaims(claimName, pairwiseSub);
        }
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>(getAdditionalConfigProperties());
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, this.getClass());
        return configProperties;
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalConfigProperties() {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(createSectorIdentifierConfig());
        configProperties.add(createHashAlgorithmConfig());
        configProperties.add(createLocalSubIdentifierConfig());
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
