package de.intension.protocol.oidc.mappers;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;

public class HmacPairwiseSubMapperHelper {

    private HmacPairwiseSubMapperHelper() {
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
        String localSubIdentifier = mappingModel.getConfig().get(HmacPairwiseSubMapper.LOCAL_SUB_IDENTIFIER_PROP_NAME);

        if ("id".equals(localSubIdentifier)) {
            return user.getId();
        }
        return user.getAttributeStream(localSubIdentifier).filter(Objects::nonNull).findFirst().orElse(null);
    }
}