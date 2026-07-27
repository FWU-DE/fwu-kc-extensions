package de.intension.authenticator;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GIVEN: a {@link RestrainingAuthenticationFlowContext} wrapping a delegate context, configured with a list of restraining IdP aliases
 * WHEN: {@link RestrainingAuthenticationFlowContext#success()} is called
 * THEN: the delegate's {@code attempted()} is called instead of {@code success()} whenever the current user is linked to one of the
 * restraining IdPs or has a "pairwiseSub" attribute, otherwise {@code success()} is called as usual.
 */
class RestrainingAuthenticationFlowContextTest {

    @Test
    void should_succeed_when_no_user_is_known() {
        var delegate = mockDelegate(null, null, null);

        wrap(delegate, "idp1").success();

        verify(delegate).success();
        verify(delegate, never()).attempted();
    }

    @Test
    void should_succeed_when_user_is_not_linked_to_restraining_idp_and_has_no_pairwise_sub() {
        var user = mock(UserModel.class);
        var delegate = mockDelegate(user, "other-idp", null);

        wrap(delegate, "idp1", "idp2").success();

        verify(delegate).success();
        verify(delegate, never()).attempted();
    }

    @Test
    void should_attempt_when_user_is_linked_to_restraining_idp() {
        var user = mock(UserModel.class);
        var delegate = mockDelegate(user, "idp1", null);

        wrap(delegate, "idp1", "idp2").success();

        verify(delegate).attempted();
        verify(delegate, never()).success();
    }

    @Test
    void should_attempt_when_user_has_pairwise_sub_attribute() {
        var user = mock(UserModel.class);
        var delegate = mockDelegate(user, "other-idp", "some-pairwise-sub");

        wrap(delegate, "idp1").success();

        verify(delegate).attempted();
        verify(delegate, never()).success();
    }

    @Test
    void should_attempt_when_user_has_no_idp_link_but_pairwise_sub_attribute() {
        var user = mock(UserModel.class);
        var delegate = mockDelegate(user, null, "some-pairwise-sub");

        wrap(delegate, "idp1").success();

        verify(delegate).attempted();
        verify(delegate, never()).success();
    }

    private RestrainingAuthenticationFlowContext wrap(AuthenticationFlowContext delegate, String... restrainingIdPs) {
        return new RestrainingAuthenticationFlowContext(delegate, List.of(restrainingIdPs));
    }

    /**
     * Mock {@link AuthenticationFlowContext} returning the passed user, whose federated identities resolve to the given linked IdP
     * alias (or none, if null), and whose "pairwiseSub" attribute resolves to the given value.
     */
    private AuthenticationFlowContext mockDelegate(UserModel user, String linkedIdpAlias, String pairwiseSub) {
        var realm = mock(RealmModel.class);
        var userProvider = mock(UserProvider.class);
        if (user != null) {
            when(user.getFirstAttribute("pairwiseSub")).thenReturn(pairwiseSub);
            Stream<FederatedIdentityModel> federatedIdentities = federatedIdentityStreamOf(linkedIdpAlias);
            when(userProvider.getFederatedIdentitiesStream(realm, user)).thenReturn(federatedIdentities);
        }
        var session = mock(KeycloakSession.class);
        when(session.users()).thenReturn(userProvider);
        var delegate = mock(AuthenticationFlowContext.class);
        when(delegate.getUser()).thenReturn(user);
        when(delegate.getRealm()).thenReturn(realm);
        when(delegate.getSession()).thenReturn(session);
        return delegate;
    }

    private Stream<FederatedIdentityModel> federatedIdentityStreamOf(String idpAlias) {
        if (idpAlias == null) {
            return Stream.empty();
        }
        var federatedIdentity = mock(FederatedIdentityModel.class);
        when(federatedIdentity.getIdentityProvider()).thenReturn(idpAlias);
        return Stream.of(federatedIdentity);
    }

}
