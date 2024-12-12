package de.intension.authenticator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.*;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AcrDenyingAuthenticatorTest {

    private boolean success = false;
    private AuthenticationFlowError failure = null;

    @BeforeEach
    public void resetMocks() {
        success = false;
        failure = null;
    }

    /**
     * GIVEN: client configured without LoA mapping for "acr"
     * WHEN: {@link AcrDenyingAuthenticator} is called for user without "acr" attribute
     * THEN: context succeeds
     */
    @Test
    void should_allow_for_client_without_loa_mapping() {
        var authenticator = authenticator(null);
        var context = mockContext();

        authenticator.authenticate(context);

        assertSuccess();
    }

    /**
     * Assertion for when everything went successful.
     */
    private void assertSuccess() {
        assertTrue(success);
        assertNull(failure);
    }

    /**
     * Mock {@link AuthenticationFlowContext} with user model having passed values in attribute "acr".
     */
    private AuthenticationFlowContext mockContext(String... acrValues) {
        var user = mock(UserModel.class);
        when(user.getAttributeStream("acr")).thenReturn(Stream.of(acrValues));
        var context = mock(AuthenticationFlowContext.class);
        when(context.getUser()).thenReturn(user);
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

    /**
     * Instantiate authenticator with mocked {@link KeycloakSession} returning client configured with passed value for LoA mapping key "acr".
     */
    private AcrDenyingAuthenticator authenticator(String acr) {
        var client = mock(ClientModel.class);
        when(client.getAttribute(Constants.ACR_LOA_MAP)).thenReturn(getClientLoaMap(acr));
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getClient()).thenReturn(client);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);
        ObjectMapper mapper = new ObjectMapper();
        return new AcrDenyingAuthenticator(session, mapper);
    }

    /**
     * Construct map for {@link Constants#ACR_LOA_MAP} with conditional passed value.
     * <hr>
     * Returns map in the format {"key1": "value1","key2": "value2"}, where:
     * <ul>
     *     <li><code>"foo": "bar"</code> is always first element</li>
     *     <li><code>"acr": "${value}"</code> is set when parameter "acr" is not null</li>
     * </ul>
     */
    private static String getClientLoaMap(String acr) {
        StringBuilder loaMapBuilder = new StringBuilder("{\"foo\":\"bar\"");
        if (acr != null) {
            loaMapBuilder.append(",\"acr\":\"").append(acr).append("\"");
        }
        return loaMapBuilder.append("}").toString();
    }
}
