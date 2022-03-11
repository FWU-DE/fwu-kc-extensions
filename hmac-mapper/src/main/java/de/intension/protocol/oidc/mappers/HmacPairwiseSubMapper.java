package de.intension.protocol.oidc.mappers;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.AbstractPairwiseSubMapper;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;

public class HmacPairwiseSubMapper extends AbstractPairwiseSubMapper
{

    public static final String  PROVIDER_ID               = "hmac";

    private static final String HASH_ALGORITHM_PROP_NAME  = "pairwiseSubHashAlgorithm";
    private static final String HASH_ALGORITHM_PROP_LABEL = "Hash algorithm";
    private static final String HASH_ALGORITHM_PROP_HELP  = "Mac hash algorithm used when calculating the pairwise subject identifier.";

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

    @Override
    public List<ProviderConfigProperty> getAdditionalConfigProperties()
    {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        var saltConfig = PairwiseSubMapperHelper.createSaltConfig();
        saltConfig.setSecret(true);
        configProperties.add(saltConfig);
        configProperties.add(createHashAlgorithmConfig());
        return configProperties;
    }

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
        if (algorithm == null) {
            throw new ProtocolMapperConfigException("Hash algorithm not available on mappingModel. Please update protocol mapper");
        }
        try {
            Mac.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ProtocolMapperConfigException("Hash algorithm '" + algorithm + "' cannot be found", e);
        }
    }

    @Override
    public String getDisplayType()
    {
        return "HMAC Pairwise subject identifier";
    }

    @Override
    public String getIdPrefix()
    {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText()
    {
        return "Calculates a pairwise subject identifier using a salted HMAC hash. See OpenID Connect specification for more info about pairwise subject identifiers.";
    }

    private static String getHashAlgorithm(ProtocolMapperModel mappingModel)
    {
        return mappingModel.getConfig().getOrDefault(HASH_ALGORITHM_PROP_NAME, null);
    }

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
}
