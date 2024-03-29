package de.intension.protocol.oidc.mappers;

import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.HMAC_SHA_256;
import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.SALT;
import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.SECTOR_IDENTIFIER;
import static de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperTest.USERNAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
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
                createMapperModel(USERNAME, EMAIL_DOMAIN), null,
                mockUserSessionModel(null, USERNAME, "tim"), null);

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
                createMapperModel(USERNAME, EMAIL_DOMAIN), null,
                mockUserSessionModel(null, USERNAME, "tim"), null);

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
                createMapperModel(USERNAME, EMAIL_DOMAIN), null,
                mockUserSessionModel(null, USERNAME, "tim"), null);

        String emailClaimValue = userInfoToken.getOtherClaims().get("email").toString();
        assertThat(emailClaimValue, not(EMAIL_BEFORE));
        assertThat(emailClaimValue, endsWith("@" + EMAIL_DOMAIN));
    }

    /**
     * GIVEN a fresh access token
     * WHEN transforming it with {@link HmacPairwiseEmailMapper}
     * AND the local sub identifier is null
     * THEN the email in the access token is unchanged
     */
    @Test
    void should_not_generate_email_for_missing_identifier() {
        HmacPairwiseEmailMapper mapper = new HmacPairwiseEmailMapper();
        var token = new AccessToken();
        token.setEmail(EMAIL_BEFORE);

        AccessToken accessToken = mapper.transformAccessToken(token,
                createMapperModel("customAttr", EMAIL_DOMAIN), null,
                mockUserSessionModel(null, "customAttr", null), null);

        assertThat(accessToken.getEmail(), equalTo(EMAIL_BEFORE));
    }

    /**
     * GIVEN a fresh access token
     * AND a user with email
     * WHEN transforming it with {@link HmacPairwiseEmailMapper}
     * AND it is configured without email domain
     * THEN the email in the access token is generated with domain of user's email
     */
    @Test
    void should_generate_email_for_missing_email_domain_config() {
        HmacPairwiseEmailMapper mapper = new HmacPairwiseEmailMapper();
        var token = new AccessToken();

        AccessToken accessToken = mapper.transformAccessToken(token,
                createMapperModel(USERNAME, null), null,
                mockUserSessionModel(null, USERNAME, "tim", EMAIL_BEFORE), null);

        assertThat(accessToken.getEmail(), not(EMAIL_BEFORE));
        assertThat(accessToken.getEmail(), not(endsWith("@" + EMAIL_DOMAIN)));
    }

    /**
     * GIVEN a fresh access token
     * AND a user without email
     * WHEN transforming it with {@link HmacPairwiseEmailMapper}
     * AND it is configured without email domain
     * THEN the email in the access token is generated without a domain
     */
    @Test
    void should_generate_email_for_missing_email_domain_config_and_user_email() {
        HmacPairwiseEmailMapper mapper = new HmacPairwiseEmailMapper();
        var token = new AccessToken();

        AccessToken accessToken = mapper.transformAccessToken(token,
                createMapperModel(USERNAME, null), null,
                mockUserSessionModel(null, USERNAME, "tim", null), null);

        assertThat(accessToken.getEmail(), not(containsString("@")));
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
        config.put("override", Boolean.TRUE.toString());
        config.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, sectorIdentifier);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, Boolean.TRUE.toString());
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, Boolean.TRUE.toString());
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, Boolean.TRUE.toString());
        protocolMapperModel.setConfig(config);
        return protocolMapperModel;
    }

    private ProtocolMapperModel createMapperModel(String localSubIdentifier, String emailDomain) {
        return createMapperModel(localSubIdentifier, HMAC_SHA_256, SALT, SECTOR_IDENTIFIER, emailDomain);
    }

    private static UserSessionModel mockUserSessionModel(String id, String localSubIdentifier,
            String localSubIdentifierValue) {
        return mockUserSessionModel(id, localSubIdentifier, localSubIdentifierValue, null);
    }

    private static UserSessionModel mockUserSessionModel(String id, String localSubIdentifier,
            String localSubIdentifierValue, String email) {
        UserSessionModel userSessionModel = mock(UserSessionModel.class);
        UserModel userModel = mock(UserModel.class);
        when(userModel.getId()).thenReturn(id);
        when(userModel.getEmail()).thenReturn(email);
        when(userModel.getAttributeStream(localSubIdentifier))
                .thenReturn(localSubIdentifierValue != null ? Arrays.asList(localSubIdentifierValue).stream()
                        : Collections.<String>emptyList().stream());
        when(userSessionModel.getUser()).thenReturn(userModel);
        return userSessionModel;
    }
}
