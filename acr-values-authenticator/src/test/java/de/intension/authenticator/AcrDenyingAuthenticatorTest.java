package de.intension.authenticator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.models.*;
import org.mockito.Mockito;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
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
     * GIVEN: client configured without LoA mappings
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
     * GIVEN: client configured with LoA mapping for "acr"
     * WHEN: {@link AcrDenyingAuthenticator} is called for user with correct "acr" attribute
     * THEN: context succeeds
     */
    @Test
    void should_allow_for_user_with_acr_attribute() {
        var authenticator = authenticator("acr");
        var context = mockContext("smth", "acr");

        authenticator.authenticate(context);

        assertSuccess();
    }

    /**
     * GIVEN: client configured with LoA mapping for "acr"
     * WHEN: {@link AcrDenyingAuthenticator} is called for user without "acr" attribute
     * THEN: context succeeds
     */
    @Test
    void should_deny_for_user_with_missing_attribute() {
        var authenticator = authenticator("acr");
        var context = mockContext();

        authenticator.authenticate(context);

        assertFailure();
    }

    /**
     * GIVEN: client configured with LoA mapping for "acr"
     * WHEN: {@link AcrDenyingAuthenticator} is called for user with incorrect "acr" attribute
     * THEN: context succeeds
     */
    @Test
    void should_deny_for_user_with_invalid_attribute() {
        var authenticator = authenticator("acr");
        var context = mockContext("bar");

        authenticator.authenticate(context);

        assertFailure();
    }

    /**
     * Assertion for when access is denied.
     */
    private void assertFailure() {
        assertFalse(success);
        assertThat(failure, equalTo(AuthenticationFlowError.ACCESS_DENIED));
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
        when(user.getId()).thenReturn("foobar");
        when(user.getAttributeStream(OAuth2Constants.ACR_VALUES)).thenReturn(Stream.of(acrValues));
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
        // needed for logging when access is denied
        var realm = mock(RealmModel.class);
        when(realm.getName()).thenReturn("test");
        when(context.getRealm()).thenReturn(realm);
        return context;
    }

    /**
     * Instantiate authenticator with mocked {@link KeycloakSession} returning client configured with passed value for LoA mapping key "acr".
     */
    private AcrDenyingAuthenticator authenticator(String acr) {
        var realm = mock(RealmModel.class);
        when(realm.getAttribute(Constants.ACR_LOA_MAP)).thenReturn(null);
        var client = mock(ClientModel.class);
        when(client.getClientId()).thenReturn("example");
        when(client.getAttribute(Constants.ACR_LOA_MAP)).thenReturn(getClientLoaMap(acr));
        when(client.getRealm()).thenReturn(realm);
        KeycloakContext context = mock(KeycloakContext.class);
        when(context.getClient()).thenReturn(client);
        KeycloakSession session = mock(KeycloakSession.class);
        when(session.getContext()).thenReturn(context);
        return new AcrDenyingAuthenticator(session);
    }

    /**
     * Construct map for {@link Constants#ACR_LOA_MAP} with conditional passed value.
     * <hr>
     * If parameter "acr" is not null it returns map in the format {"key1": value1,"key2": value2}, where:
     * <ul>
     *     <li><code>"foo": 1337</code></li>
     *     <li><code>"${acr}": 420</code></li>
     * </ul>
     * If parameter "acr" is null it returns an empty map.
     */
    private static String getClientLoaMap(String acr) {
        if (acr != null) {
            return "{\"foo\":1337" + ",\"" + acr + "\":420}";
        }
        return "{}";
    }
}
