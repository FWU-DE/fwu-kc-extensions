package de.intension.protocol.oidc.mappers;

import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.HMAC_SHA_256;
import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.SALT;
import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.SECTOR_IDENTIFIER;
import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.USER_ID;
import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.USER_NAME;
import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.mockUserSessionModel;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

class HmacPairwiseEmailMapperTest {

    private static final String EMAIL_DOMAIN = "vidis.test";
    private static final String EMAIL_BEFORE = "example@test.de";

    /**
     * GIVEN a fresh access token
     * WHEN transforming it with {@link HmacPairwiseEmailMapper}
     * THEN the email in the access token is set to a pseudo value
     */
    @Test
    void should_generate_email_for_access_token() {
        HmacPairwiseEmailMapper mapper = new HmacPairwiseEmailMapper();
        var token = new AccessToken();
        token.setEmail(EMAIL_BEFORE);

        AccessToken accessToken = mapper.transformAccessToken(token,
                createMapperModel(USER_NAME, EMAIL_DOMAIN), null,
                mockUserSessionModel(USER_ID, USER_NAME, "tim"), null);

        assertThat(accessToken.getEmail(), not(EMAIL_BEFORE));
        assertThat(accessToken.getEmail(), endsWith("@" + EMAIL_DOMAIN));
    }

    /**
     * GIVEN a fresh ID token
     * WHEN transforming it with {@link HmacPairwiseEmailMapper}
     * THEN the email in the ID token is set to a pseudo value
     */
    @Test
    void should_generate_email_for_ID_token() {
        HmacPairwiseEmailMapper mapper = new HmacPairwiseEmailMapper();
        var token = new IDToken();
        token.setEmail(EMAIL_BEFORE);

        IDToken idToken = mapper.transformIDToken(token,
                createMapperModel(USER_NAME, EMAIL_DOMAIN), null,
                mockUserSessionModel(USER_ID, USER_NAME, "tim"), null);

        assertThat(idToken.getEmail(), not(EMAIL_BEFORE));
        assertThat(idToken.getEmail(), endsWith("@" + EMAIL_DOMAIN));
    }

    /**
     * GIVEN a fresh user info token
     * WHEN transforming it with {@link HmacPairwiseEmailMapper}
     * THEN the email in the user info token is set to a pseudo value
     */
    @Test
    void should_generate_email_for_user_info_token() {
        HmacPairwiseEmailMapper mapper = new HmacPairwiseEmailMapper();
        var token = new AccessToken();
        token.setOtherClaims("email", EMAIL_BEFORE);
        
        AccessToken userInfoToken = mapper.transformUserInfoToken(new AccessToken(),
                createMapperModel(USER_NAME, EMAIL_DOMAIN), null,
                mockUserSessionModel(USER_ID, USER_NAME, "tim"), null);

        String emailClaimValue = userInfoToken.getOtherClaims().get("email").toString();
        assertThat(emailClaimValue, not(EMAIL_BEFORE));
        assertThat(emailClaimValue, endsWith("@" + EMAIL_DOMAIN));
    }

    /**
     * Create Protocol mapper model with the local sub identifier passed
     */
    private ProtocolMapperModel createMapperModel(String localSubIdentifier, String hashAlgorithm, String salt,
            String sectorIdentifier, String emailDomain) {
        ProtocolMapperModel protocolMapperModel = new ProtocolMapperModel();
        protocolMapperModel.setConfig(new HashMap<String, String>());
        protocolMapperModel.setName("HMAC Mapper");
        Map<String, String> config = new HashMap<>();
        config.put("pairwiseSubHashAlgorithm", hashAlgorithm);
        config.put("pairwiseSubAlgorithmSalt", salt);
        config.put("pairwiseLocalSubIdentifier", localSubIdentifier);
        config.put("emailDomain", emailDomain);
        config.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, sectorIdentifier);
        protocolMapperModel.setConfig(config);
        return protocolMapperModel;
    }

    private ProtocolMapperModel createMapperModel(String localSubIdentifier, String emailDomain) {
        return createMapperModel(localSubIdentifier, HMAC_SHA_256, SALT, SECTOR_IDENTIFIER, emailDomain);
    }
}
