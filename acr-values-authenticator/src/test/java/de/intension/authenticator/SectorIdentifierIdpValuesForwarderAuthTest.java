package de.intension.authenticator;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static de.intension.authenticator.SectorIdentifierIdpValuesForwarderAuthFactory.SECTOR_IDENTIFIER_PARAM_NAME;
import static de.intension.authenticator.SectorIdentifierIdpValuesForwarderAuthFactory.SECTOR_IDENTIFIER_URI_NOTE;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;
import static org.mockito.Mockito.*;

class SectorIdentifierIdpValuesForwarderAuthTest {

    private static final String CLIENT_ID    = "some-client";
    private static final String PARAM_NAME   = "sector_identifier_uri";
    private static final String SECTOR_URI   = "https://sector.identifier.de";

    private final SectorIdentifierIdpValuesForwarderAuth authenticator = new SectorIdentifierIdpValuesForwarderAuth();

    private boolean success = false;

    @BeforeEach
    void reset() {
        success = false;
    }

    /**
     * GIVEN: client has an HMAC pairwise subject mapper configured with a sector identifier URI
     * WHEN: authenticator is called with the sector identifier param name configured
     * THEN: sector identifier URI is stored as additional request param client note, as well as under the
     * dedicated note used for post-login verification, and context succeeds
     */
    @Test
    void should_forward_sector_identifier_uri_when_mapper_configured() {
        AuthenticationSessionModel session = mockSession(mockClient(SECTOR_URI));
        AuthenticationFlowContext context = mockContext(session, PARAM_NAME);

        authenticator.authenticate(context);

        verify(session).setClientNote(LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + PARAM_NAME, SECTOR_URI);
        verify(session).setClientNote(SECTOR_IDENTIFIER_URI_NOTE, SECTOR_URI);
        assertTrue(success);
    }

    /**
     * GIVEN: client has no HMAC pairwise subject mapper configured
     * WHEN: authenticator is called with the sector identifier param name configured
     * THEN: no client note is set and context still succeeds
     */
    @Test
    void should_not_forward_when_no_hmac_mapper_configured() {
        ClientModel client = mock(ClientModel.class);
        when(client.getClientId()).thenReturn(CLIENT_ID);
        when(client.getProtocolMappersStream()).thenReturn(Stream.empty());
        AuthenticationSessionModel session = mockSession(client);
        AuthenticationFlowContext context = mockContext(session, PARAM_NAME);

        authenticator.authenticate(context);

        verify(session, never()).setClientNote(anyString(), anyString());
        assertTrue(success);
    }

    /**
     * GIVEN: client has an HMAC pairwise subject mapper without a configured sector identifier URI
     * WHEN: authenticator is called with the sector identifier param name configured
     * THEN: no client note is set and context still succeeds
     */
    @Test
    void should_not_forward_when_sector_identifier_uri_missing() {
        AuthenticationSessionModel session = mockSession(mockClient(null));
        AuthenticationFlowContext context = mockContext(session, PARAM_NAME);

        authenticator.authenticate(context);

        verify(session, never()).setClientNote(anyString(), anyString());
        assertTrue(success);
    }

    /**
     * GIVEN: authenticator has no configured param name
     * WHEN: authenticator is called
     * THEN: the dedicated note for post-login verification is still set, but no additional request param
     * client note is set, and context still succeeds
     */
    @Test
    void should_not_forward_when_param_name_not_configured() {
        AuthenticationSessionModel session = mockSession(mockClient(SECTOR_URI));
        AuthenticationFlowContext context = mockContext(session, null);

        authenticator.authenticate(context);

        verify(session).setClientNote(SECTOR_IDENTIFIER_URI_NOTE, SECTOR_URI);
        verify(session, never()).setClientNote(startsWith(LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX), anyString());
        assertTrue(success);
    }

    private ClientModel mockClient(String sectorIdentifierUri) {
        ProtocolMapperModel mapper = mock(ProtocolMapperModel.class);
        when(mapper.getProtocolMapper()).thenReturn(HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID);
        Map<String, String> config = new HashMap<>();
        if (sectorIdentifierUri != null) {
            config.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, sectorIdentifierUri);
        }
        when(mapper.getConfig()).thenReturn(config);

        ClientModel client = mock(ClientModel.class);
        when(client.getClientId()).thenReturn(CLIENT_ID);
        when(client.getProtocolMappersStream()).thenReturn(Stream.of(mapper));
        return client;
    }

    private AuthenticationSessionModel mockSession(ClientModel client) {
        AuthenticationSessionModel session = mock(AuthenticationSessionModel.class);
        when(session.getClient()).thenReturn(client);
        return session;
    }

    private AuthenticationFlowContext mockContext(AuthenticationSessionModel session, String configuredParamName) {
        AuthenticationFlowContext context = mock(AuthenticationFlowContext.class);
        when(context.getAuthenticationSession()).thenReturn(session);
        if (configuredParamName != null) {
            AuthenticatorConfigModel configModel = mock(AuthenticatorConfigModel.class);
            when(configModel.getConfig()).thenReturn(Map.of(SECTOR_IDENTIFIER_PARAM_NAME, configuredParamName));
            when(context.getAuthenticatorConfig()).thenReturn(configModel);
        } else {
            when(context.getAuthenticatorConfig()).thenReturn(null);
        }
        doAnswer(x -> {
            success = true;
            return null;
        }).when(context).success();
        return context;
    }
}
