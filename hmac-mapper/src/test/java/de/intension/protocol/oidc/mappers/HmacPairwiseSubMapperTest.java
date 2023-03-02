package de.intension.protocol.oidc.mappers;

import static org.junit.jupiter.api.Assertions.*;
import static org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

class HmacPairwiseSubMapperTest
{

    private static final String ID                = "id";
    static final String         USERNAME          = "username";
    private static final String USER_ID           = "608b8580-9bcd-4723-be12-1affd60bcc3a";
    static final String         SECTOR_IDENTIFIER = "http://a-static-url.de/sector_identifiers.json";
    static final String         HMAC_SHA_256      = "HmacSHA256";
    static final String         SALT              = "P5ZD+fqPLDTW";

    /**
     * GIVEN: a user, same salt, hash algorithm, sector identifier
     * WHEN: sub created twice for access token with same user with same local sub
     * value
     * THEN: resulting subject value is same
     */
    @Test
    void should_generate_same_subject_value_when_same_local_sub_identifier_value()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();

        AccessToken accessToken = mapper.transformAccessToken(new AccessToken(), createMapperModel(USERNAME), null,
                                                              mockUserSessionModel(USER_ID, USERNAME, "tim"), null);
        AccessToken accessToken2 = mapper.transformAccessToken(new AccessToken(), createMapperModel(USERNAME), null,
                                                               mockUserSessionModel(USER_ID, USERNAME, "tim"), null);

        assertEquals(accessToken.getSubject(), accessToken2.getSubject());
    }

    /**
     * GIVEN: a user, same salt, hash algorithm, sector identifier
     * WHEN: sub created twice for access token with same user with different local
     * sub value
     * THEN: resulting subject value is not same
     */
    @Test
    void should_generate_different_subject_value_when_different_local_sub_identifier_value()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();

        AccessToken accessToken = mapper.transformAccessToken(new AccessToken(), createMapperModel(USERNAME), null,
                                                              mockUserSessionModel(USER_ID, USERNAME, "tim"), null);
        AccessToken accessToken2 = mapper.transformAccessToken(new AccessToken(), createMapperModel(ID), null,
                                                               mockUserSessionModel(USER_ID, USERNAME, USER_ID), null);

        assertNotEquals(accessToken.getSubject(), accessToken2.getSubject());
    }

    /**
     * GIVEN: a user, same salt, hash algorithm, sector identifier
     * WHEN: sub created for id token twice with same user with same local sub value
     * THEN: resulting subject value is same
     */
    @Test
    void should_generate_same_subject_value_for_id_token_when_same_local_sub_identifier_value()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();

        IDToken idToken = mapper.transformIDToken(new IDToken(), createMapperModel(USERNAME), null,
                                                  mockUserSessionModel(USER_ID, USERNAME, "tim"), null);
        IDToken idToken2 = mapper.transformIDToken(new IDToken(), createMapperModel(USERNAME), null,
                                                   mockUserSessionModel(USER_ID, USERNAME, "tim"), null);

        assertEquals(idToken.getSubject(), idToken2.getSubject());
    }

    /**
     * GIVEN: a user, same salt, hash algorithm, sector identifier
     * WHEN: sub created for user info token twice with same user with same local
     * sub value
     * THEN: resulting sub in claim is same
     */
    @Test
    void should_generate_same_subject_value_for_user_info_token_when_same_local_sub_identifier_value()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();

        AccessToken accessToken = mapper.transformUserInfoToken(new AccessToken(), createMapperModel(USERNAME), null,
                                                                mockUserSessionModel(USER_ID, USERNAME, "tim"), null);
        AccessToken accessToken2 = mapper.transformUserInfoToken(new AccessToken(), createMapperModel(USERNAME), null,
                                                                 mockUserSessionModel(USER_ID, USERNAME, "tim"), null);

        assertEquals(accessToken.getOtherClaims().get("sub"), accessToken2.getOtherClaims().get("sub"));
    }

    /**
     * GIVEN: a user, same salt, hash algorithm, sector identifier
     * WHEN: sub created for missing local sub identifier
     * THEN: sub is not changed on the access token
     */
    @Test
    void should_throw_run_time_exception_when_empty_local_sub_identifier_value()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        AccessToken token = new AccessToken().subject("before");

        mapper.transformAccessToken(token, createMapperModel("wrongLocalSubIdentifier"), null,
                                    mockUserSessionModel(USER_ID, "wrongLocalSubIdentifier", null), null);

        assertEquals("before", token.getSubject());

    }

    /**
     * GIVEN: a user, different salt, hash algorithm, sector identifier
     * WHEN: sub created twice with same user with different local sub value
     * THEN: resulting subject value is not same
     */
    @Test
    void should_generate_different_subject_value_when_different_salt()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();

        AccessToken accessToken = mapper.transformAccessToken(new AccessToken(), createMapperModel(USERNAME), null,
                                                              mockUserSessionModel(USER_ID, USERNAME, "tim"), null);
        ProtocolMapperModel anotherSaltProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, "Azhdfopek",
                                                                          SECTOR_IDENTIFIER);
        AccessToken accessToken2 = mapper.transformAccessToken(new AccessToken(), anotherSaltProtocolMapper, null,
                                                               mockUserSessionModel(USER_ID, USERNAME, "tim"), null);

        assertNotEquals(accessToken.getSubject(), accessToken2.getSubject());
    }

    /**
     * GIVEN: a user, same salt, hash algorithm, sector identifier
     * WHEN: sub created twice with same user with different sector identifier sub
     * value
     * THEN: resulting subject value is not same
     */
    @Test
    void should_generate_different_subject_value_when_different_sectorIdentifier()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();

        AccessToken accessToken = mapper.transformAccessToken(new AccessToken(), createMapperModel(USERNAME), null,
                                                              mockUserSessionModel(USER_ID, USERNAME, "tim"), null);
        ProtocolMapperModel anotherSaltProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, SALT,
                                                                          "http://www.example.de");
        AccessToken accessToken2 = mapper.transformAccessToken(new AccessToken(), anotherSaltProtocolMapper, null,
                                                               mockUserSessionModel(USER_ID, USERNAME, "tim"), null);

        assertNotEquals(accessToken.getSubject(), accessToken2.getSubject());
    }

    /**
     * GIVEN: hmac pairwise sub mapper without salt config
     * WHEN: sub is generated
     * THEN: IllegalStateException is thrown with expected message
     */
    @Test
    void should_throw_illegal_state_exception_when_salt_not_configured()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        ProtocolMapperModel noSaltProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, null, SECTOR_IDENTIFIER);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> mapper.generateIdentifier(noSaltProtocolMapper, USER_ID));

        assertEquals("Salt not available on mappingModel. Please update protocol mapper", exception.getMessage());
    }

    /**
     * GIVEN: hmac pairwise sub mapper with wrong algorithm configured
     * WHEN: sub is generated
     * THEN: IllegalStateException is thrown with expected message
     */
    @Test
    void should_throw_illegal_state_exception_when_wrong_algorithm_configured()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        ProtocolMapperModel wrongAlgorithmProtocolMapper = createMapperModel(USERNAME, "wrongAlgorithm", SALT,
                                                                             SECTOR_IDENTIFIER);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> mapper.generateIdentifier(wrongAlgorithmProtocolMapper, USER_ID));

        assertEquals("Generating sub failed", exception.getMessage());
    }

    /**
     * GIVEN: hmac pairwise sub mapper with no sector identifier configured
     * WHEN: mapper config is validated
     * THEN: ProtocolMapperConfigException is thrown with expected message
     */
    @Test
    void should_throw_protocol_mapper_config_exception_when_no_sector_identifier_configured()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        ProtocolMapperModel noSectorIdentifierProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, SALT, null);

        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                                                               () -> mapper.validateSectorIdentifier(null, null, noSectorIdentifierProtocolMapper));

        assertEquals("Sector Identifier must not be null or empty.", exception.getMessage());
    }

    /**
     * GIVEN: hmac pairwise sub mapper with empty sector identifier configured
     * WHEN: mapper config is validated
     * THEN: ProtocolMapperConfigException is thrown with expected message
     */
    @Test
    void should_throw_protocol_mapper_config_exception_when_empty_sector_identifier_configured()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        ProtocolMapperModel emptySectorIdentifierProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, SALT, "");

        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                                                               () -> mapper.validateSectorIdentifier(null, null, emptySectorIdentifierProtocolMapper));

        assertEquals("Sector Identifier must not be null or empty.", exception.getMessage());
    }

    /**
     * GIVEN: hmac pairwise sub mapper with invalid sector identifier configured
     * WHEN: mapper config is validated
     * THEN: ProtocolMapperConfigException is thrown with expected message
     */
    @Test
    void should_throw_protocol_mapper_config_exception_when_invalid_sector_identifier_configured()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        ProtocolMapperModel invalidSectorIdentifierProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, SALT,
                                                                                      "invalidSectorIdentifier");

        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                                                               () -> mapper.validateSectorIdentifier(null, null, invalidSectorIdentifierProtocolMapper));

        assertEquals("Invalid Sector Identifier URI.", exception.getMessage());
    }

    /**
     * GIVEN: hmac pairwise sub mapper with malformed sector identifier configured
     * WHEN: mapper config is validated
     * THEN: ProtocolMapperConfigException is thrown with expected message
     */
    @Test
    void should_throw_protocol_mapper_config_exception_when_malformed_sector_identifier_configured()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        ProtocolMapperModel malformedSectorIdentifierProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, SALT,
                                                                                        "http://finance.yahoo.com/q/h?s=^IXIC");

        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                                                               () -> mapper.validateSectorIdentifier(null, null, malformedSectorIdentifierProtocolMapper));

        assertEquals("Invalid Sector Identifier URI.", exception.getMessage());
    }

    /**
     * GIVEN: hmac pairwise sub mapper with wrong algorithm
     * WHEN: mapper config is validated
     * THEN: ProtocolMapperConfigException is thrown with expected message
     */
    @Test
    void should_throw_protocol_mapper_config_exception_when_wrong_algorithm_configured()
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        ProtocolMapperModel noAlgorithmProtocolMapper = createMapperModel(USERNAME, "wrongAlgorithm", SALT,
                                                                          SECTOR_IDENTIFIER);

        ProtocolMapperConfigException exception = assertThrows(ProtocolMapperConfigException.class,
                                                               () -> mapper.validateConfig(null, null, null, noAlgorithmProtocolMapper));

        assertEquals("Hash algorithm 'wrongAlgorithm' cannot be found", exception.getMessage());
    }

    /**
     * GIVEN: hmac pairwise sub mapper with no salt
     * WHEN: mapper config is validated
     * THEN: salt is generated and set to mapper model
     */
    @Test
    void should_generate_salt_when_none_configured()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        ProtocolMapperModel noSaltProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, null, SECTOR_IDENTIFIER);

        mapper.validateConfig(null, null, null, noSaltProtocolMapper);

        assertNotNull(noSaltProtocolMapper.getConfig().get(PAIRWISE_SUB_ALGORITHM_SALT));
    }

    /**
     * GIVEN: hmac pairwise sub mapper with empty salt value
     * WHEN: mapper config is validated
     * THEN: salt is generated and set to mapper model
     */
    @Test
    void should_generate_salt_when_empty_salt_configured()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        ProtocolMapperModel emptySaltProtocolMapper = createMapperModel(USERNAME, HMAC_SHA_256, "", SECTOR_IDENTIFIER);

        mapper.validateConfig(null, null, mock(ClientModel.class), emptySaltProtocolMapper);

        assertNotNull(emptySaltProtocolMapper.getConfig().get(PAIRWISE_SUB_ALGORITHM_SALT));
    }

    /**
     * GIVEN: valid hmac pairwise sub mapper
     * WHEN: get config properties
     * THEN: contains 4 config properties
     */
    @Test
    void should_contain_config_properties_when_configured()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();

        List<ProviderConfigProperty> configProperties = mapper.getConfigProperties();

        assertEquals(7, configProperties.size());
    }

    /**
     * GIVEN: valid hmac pairwise sub mapper
     * WHEN: get id of the mapper
     * THEN: value of the result is concatenated value of
     * oidc-{prefix-id}-{pairwise-mapper-suffix}
     */
    @Test
    void should_contain_contenated_id_of_hmac_pairwise_mapper()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        assertEquals("oidc-hmac-pairwise-subject-mapper", mapper.getId());
    }

    /**
     * GIVEN: valid hmac pairwise sub mapper
     * WHEN: get display category
     * THEN: it is Token mapper
     */
    @Test
    void should_have_token_mapper_category_for_hmac_pairwise_mapper()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        assertEquals(AbstractOIDCProtocolMapper.TOKEN_MAPPER_CATEGORY, mapper.getDisplayCategory());
    }

    /**
     * GIVEN: valid hmac pairwise sub mapper
     * WHEN: get display type
     * THEN: it is 'HMAC Pairwise subject with static sectorIdentifier'
     */
    @Test
    void should_have_expected_display_type_for_hmac_pairwise_mapper()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        assertEquals("HMAC Pairwise subject with static sectorIdentifier", mapper.getDisplayType());
    }

    /**
     * GIVEN: valid hmac pairwise sub mapper
     * WHEN: get help text
     * THEN: it is 'Calculates a pairwise subject identifier using a salted HMAC
     * hash and
     * sectorIdentifier. See OpenID Connect specification for more info about
     * pairwise subject
     * identifiers.'
     */
    @Test
    void should_have_expected_help_text_for_hmac_pairwise_mapper()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        assertEquals(
                     "Calculates a pairwise subject identifier using a salted HMAC hash and sectorIdentifier. See OpenID Connect specification for more info about pairwise subject identifiers.",
                     mapper.getHelpText());
    }

    /**
     * Create Protocol mapper model with the local sub identifier passed
     *
     * @param localSubIdentifier
     * @param hashAlgorithm
     * @param salt
     * @param sectorIdentifier
     * @return
     */
    private ProtocolMapperModel createMapperModel(String localSubIdentifier, String hashAlgorithm, String salt,
                                                  String sectorIdentifier)
    {
        ProtocolMapperModel protocolMapperModel = new ProtocolMapperModel();
        protocolMapperModel.setConfig(new HashMap<String, String>());
        protocolMapperModel.setName("HMAC Mapper");
        Map<String, String> config = new HashMap<>();
        config.put("pairwiseSubHashAlgorithm", hashAlgorithm);
        config.put("pairwiseSubAlgorithmSalt", salt);
        config.put("pairwiseLocalSubIdentifier", localSubIdentifier);
        config.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, sectorIdentifier);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, Boolean.TRUE.toString());
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, Boolean.TRUE.toString());
        protocolMapperModel.setConfig(config);
        return protocolMapperModel;
    }

    private ProtocolMapperModel createMapperModel(String localSubIdentifier)
    {
        return createMapperModel(localSubIdentifier, HMAC_SHA_256, SALT, SECTOR_IDENTIFIER);
    }

    private static UserSessionModel mockUserSessionModel(String id, String localSubIdentifier,
                                                         String localSubIdentifierValue)
    {
        UserSessionModel userSessionModel = mock(UserSessionModel.class);
        UserModel userModel = mock(UserModel.class);
        when(userModel.getId()).thenReturn(id);
        when(userModel.getAttributeStream(localSubIdentifier))
            .thenReturn(localSubIdentifierValue != null ? Stream.of(localSubIdentifierValue)
                    : Stream.empty());
        when(userSessionModel.getUser()).thenReturn(userModel);
        return userSessionModel;
    }
}