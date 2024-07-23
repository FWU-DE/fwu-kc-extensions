package de.intension.protocol.oidc.mappers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;

import java.util.HashMap;
import java.util.Map;

import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.*;
import static org.junit.jupiter.api.Assertions.*;

class HmacSimpleUserAttributeMapperTest {
    private static final String USER_ID = "505fb0d6-bce1-3323-9ae2-60b47dad7cac";

    /**
     * GIVEN: a user, hash algorithm, sector identifier
     * WHEN: sub created twice for access token with same user with same local sub value
     * THEN: resulting claim value is same
     */
    @Test
    void should_generate_same_claim_value_when_same_local_sub_identifier_value() {
        var mapper = new HmacSimpleUserAttributeMapper();

        AccessToken accessToken = mapper.transformAccessToken(new AccessToken(), createMapperModel(USERNAME), null,
                mockUserSessionModel(USER_ID, USERNAME, "tom"), null);
        AccessToken accessToken2 = mapper.transformAccessToken(new AccessToken(), createMapperModel(USERNAME), null,
                mockUserSessionModel(USER_ID, USERNAME, "tom"), null);

        assertEquals(accessToken.getOtherClaims().get("clamClaim"), accessToken2.getOtherClaims().get("clamClaim"));
    }

    /**
     * GIVEN: a user, hash algorithm, sector identifier
     * WHEN: sub created twice with same user with different sector identifier sub value
     * THEN: resulting claim value is not same
     */
    @Test
    void should_generate_different_claim_value_when_different_sectorIdentifier() {
        var mapper = new HmacSimpleUserAttributeMapper();

        AccessToken accessToken = mapper.transformAccessToken(new AccessToken(), createMapperModel(USERNAME, HMAC_SHA_256, "string1"), null,
                mockUserSessionModel(USER_ID, USERNAME, "tom"), null);
        AccessToken accessToken2 = mapper.transformAccessToken(new AccessToken(), createMapperModel(USERNAME, HMAC_SHA_256, "string2"), null,
                mockUserSessionModel(USER_ID, USERNAME, "tom"), null);

        assertNotEquals(accessToken.getOtherClaims().get("clamClaim"), accessToken2.getOtherClaims().get("clamClaim"));
    }

    /**
     * GIVEN: a user, hash algorithm, sector identifier
     * WHEN: sub created for missing local sub identifier
     * THEN: sub is not changed on the access token
     */
    @Test
    void should_ignore_when_empty_local_sub_identifier_value() {
        var mapper = new HmacSimpleUserAttributeMapper();
        AccessToken token = new AccessToken();
        token.setOtherClaims("clamClaim", "before");

        mapper.transformAccessToken(token, createMapperModel("wrongLocalSubIdentifier"), null,
                mockUserSessionModel(USER_ID, "wrongLocalSubIdentifier", null), null);

        assertEquals("before", token.getOtherClaims().get("clamClaim"));
    }

    /**
     * GIVEN: hmac simple sub mapper with empty sector identifier configured
     * WHEN: mapper config is validated
     * THEN: ProtocolMapperConfigException is thrown with expected message
     */
    @ParameterizedTest
    @NullAndEmptySource
    void should_throw_protocol_mapper_config_exception_when_empty_sector_identifier_configured(String localSubIdentifier) {
        var mapper = new HmacSimpleUserAttributeMapper();
        ProtocolMapperModel emptySectorIdentifierProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, localSubIdentifier);

        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                () -> mapper.validateSectorIdentifier(null, null, emptySectorIdentifierProtocolMapper));

        assertEquals("Sector Identifier must not be null or empty.", exception.getMessage());
    }

    /**
     * GIVEN: hmac pairwise sub mapper with wrong algorithm configured
     * WHEN: sub is generated
     * THEN: IllegalStateException is thrown with expected message
     */
    @Test
    void should_throw_illegal_state_exception_when_wrong_algorithm_configured() {
        var mapper = new HmacSimpleUserAttributeMapper();
        ProtocolMapperModel wrongAlgorithmProtocolMapper = createMapperModel(USERNAME, "wrongAlgorithm", SECTOR_IDENTIFIER);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> mapper.generateIdentifier(wrongAlgorithmProtocolMapper, USER_ID));

        assertEquals("Generating sub failed", exception.getMessage());
    }

    /**
     * Create Protocol mapper model with the local sub identifier passed
     */
    private ProtocolMapperModel createMapperModel(String localSubIdentifier, String hashAlgorithm, String sectorIdentifier) {
        ProtocolMapperModel protocolMapperModel = new ProtocolMapperModel();
        protocolMapperModel.setConfig(new HashMap<>());
        protocolMapperModel.setName("HMAC Mapper");
        Map<String, String> config = new HashMap<>();
        config.put("pairwiseSubHashAlgorithm", hashAlgorithm);
        config.put("pairwiseLocalSubIdentifier", localSubIdentifier);
        config.put(HmacSimpleUserAttributeMapper.SECTOR_IDENTIFIER_PROP_NAME, sectorIdentifier);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, Boolean.TRUE.toString());
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, Boolean.TRUE.toString());
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "clamClaim");
        protocolMapperModel.setConfig(config);
        return protocolMapperModel;
    }

    private ProtocolMapperModel createMapperModel(String localSubIdentifier) {
        return createMapperModel(localSubIdentifier, HMAC_SHA_256, "random string");
    }
}
