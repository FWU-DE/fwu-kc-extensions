package de.intension.authenticator;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static de.intension.authenticator.SectorIdentifierIdpValuesForwarderAuthFactory.SECTOR_IDENTIFIER_URI_NOTE;
import static de.intension.authenticator.SectorIdentifierVerifierAuthenticatorFactory.USER_ATTRIBUTE_NAME_CONFIG;
import static de.intension.authenticator.SectorIdentifierVerifierAuthenticatorFactory.USER_ATTRIBUTE_NAME_DEFAULT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SectorIdentifierVerifierAuthenticatorTest {

    private static final String SECTOR_URI = "https://sector.identifier.de";
    private static final String EXTERNAL_SUB_ATTRIBUTE = "idpPseudonym";

    private final SectorIdentifierVerifierAuthenticator authenticator = new SectorIdentifierVerifierAuthenticator();

    private boolean success = false;
    private AuthenticationFlowError failure = null;

    @BeforeEach
    void reset() {
        success = false;
        failure = null;
    }

    /**
     * GIVEN: no sector identifier URI note present in the authentication session
     * WHEN: authenticator is called
     * THEN: verification is skipped and context succeeds
     */
    @Test
    void should_succeed_when_no_note_present() {
        AuthenticationFlowContext context = mockContext(null, mockClient(null), SECTOR_URI);

        authenticator.authenticate(context);

        assertSuccess();
    }

    /**
     * GIVEN: sector identifier URI note present and user attribute matches
     * WHEN: authenticator is called
     * THEN: context succeeds
     */
    @Test
    void should_succeed_when_attribute_matches() {
        AuthenticationFlowContext context = mockContext(SECTOR_URI, mockClient(null), SECTOR_URI);

        authenticator.authenticate(context);

        assertSuccess();
    }

    /**
     * GIVEN: sector identifier URI note present but user attribute value does not match
     * WHEN: authenticator is called
     * THEN: context fails with ACCESS_DENIED
     */
    @Test
    void should_fail_when_attribute_does_not_match() {
        AuthenticationFlowContext context = mockContext(SECTOR_URI, mockClient(null), "https://different.identifier.de");

        authenticator.authenticate(context);

        assertFailure();
    }

    /**
     * GIVEN: sector identifier URI note present, user has no value for the sector identifier attribute,
     * AND the client's HMAC pairwise subject mapper has no external sub attribute configured
     * WHEN: authenticator is called
     * THEN: the IdP is considered to have ignored the sector identifier entirely, so context succeeds
     * (Keycloak falls back to generating its own pseudonym)
     */
    @Test
    void should_succeed_when_attribute_missing_and_no_pseudonym_configured() {
        AuthenticationFlowContext context = mockContext(SECTOR_URI, mockClient(null));

        authenticator.authenticate(context);

        assertSuccess();
    }

    /**
     * GIVEN: sector identifier URI note present, user has no value for the sector identifier attribute,
     * AND the client's HMAC pairwise subject mapper has an external sub attribute configured, but the
     * user has no non-blank value for it either
     * WHEN: authenticator is called
     * THEN: no pseudonym was mapped either, so context succeeds
     */
    @Test
    void should_succeed_when_attribute_and_pseudonym_both_missing() {
        ClientModel client = mockClient(EXTERNAL_SUB_ATTRIBUTE);
        AuthenticationFlowContext context = mockContext(SECTOR_URI, client);
        stubPseudonymAttribute(context, null);

        authenticator.authenticate(context);

        assertSuccess();
    }

    /**
     * GIVEN: sector identifier URI note present, user has no value for the sector identifier attribute,
     * BUT the client's HMAC pairwise subject mapper has an external sub attribute configured AND the user
     * has a non-blank value for it (i.e. the IdP did send back a pseudonym, just not the sector identifier)
     * WHEN: authenticator is called
     * THEN: context fails with ACCESS_DENIED, since this is an inconsistent/suspicious state
     */
    @Test
    void should_fail_when_attribute_missing_but_pseudonym_present() {
        ClientModel client = mockClient(EXTERNAL_SUB_ATTRIBUTE);
        AuthenticationFlowContext context = mockContext(SECTOR_URI, client);
        stubPseudonymAttribute(context, "some-pseudonym-value");

        authenticator.authenticate(context);

        assertFailure();
    }

    private void assertFailure() {
        assertFalse(success);
        assertThat(failure, equalTo(AuthenticationFlowError.ACCESS_DENIED));
    }

    private void assertSuccess() {
        assertTrue(success);
        assertNull(failure);
    }

    private ClientModel mockClient(String externalSubAttribute) {
        ClientModel client = mock(ClientModel.class);
        if (externalSubAttribute == null) {
            when(client.getProtocolMappersStream()).thenReturn(Stream.empty());
            return client;
        }
        ProtocolMapperModel mapper = mock(ProtocolMapperModel.class);
        when(mapper.getProtocolMapper()).thenReturn(HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID);
        Map<String, String> config = new HashMap<>();
        config.put(HmacPairwiseSubMapperHelper.EXTERNAL_SUB_ATTRIBUTE_PROP_NAME, externalSubAttribute);
        when(mapper.getConfig()).thenReturn(config);
        when(client.getProtocolMappersStream()).thenReturn(Stream.of(mapper));
        return client;
    }

    private void stubPseudonymAttribute(AuthenticationFlowContext context, String pseudonymValue) {
        when(context.getUser().getAttributeStream(EXTERNAL_SUB_ATTRIBUTE))
                .thenReturn(pseudonymValue != null ? Stream.of(pseudonymValue) : Stream.empty());
    }

    private AuthenticationFlowContext mockContext(String sentSectorIdentifierUri, ClientModel client, String... attributeValues) {
        AuthenticationSessionModel session = mock(AuthenticationSessionModel.class);
        when(session.getClientNote(SECTOR_IDENTIFIER_URI_NOTE)).thenReturn(sentSectorIdentifierUri);
        when(session.getClient()).thenReturn(client);

        AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);
        when(context.getAuthenticationSession()).thenReturn(session);
        if (sentSectorIdentifierUri != null) {
            UserModel user = mock(UserModel.class);
            when(user.getId()).thenReturn("foobar");
            when(user.getAttributeStream(USER_ATTRIBUTE_NAME_DEFAULT)).thenReturn(Stream.of(attributeValues));
            when(context.getUser()).thenReturn(user);
            AuthenticatorConfigModel configModel = mock(AuthenticatorConfigModel.class);
            when(configModel.getConfig()).thenReturn(Map.of(USER_ATTRIBUTE_NAME_CONFIG, USER_ATTRIBUTE_NAME_DEFAULT));
            when(context.getAuthenticatorConfig()).thenReturn(configModel);
        }
        doAnswer(x -> {
            success = true;
            return null;
        }).when(context).success();
        doAnswer(x -> {
            failure = x.getArgument(0);
            return null;
        }).when(context).failure(Mockito.any());
        return context;
    }
}
