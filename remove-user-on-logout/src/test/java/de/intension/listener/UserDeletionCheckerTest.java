package de.intension.listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.UserModel;
import org.mockito.junit.jupiter.MockitoExtension;

import de.intension.resources.admin.DeletableUserType;

@ExtendWith(MockitoExtension.class)
class UserDeletionCheckerTest {

    @Test
    void shouldBeDeletableGivenUserHasNoFederationLinkButIdpAlias() {
        // given
        UserModel user = mock(UserModel.class);
        when(user.getFederationLink()).thenReturn(null);
        when(user.getAttributes()).thenReturn(Map.of("idpAlias", List.of("idpAlias")));

        // when
        boolean result = UserDeletionChecker.userShouldBeDeleted(user, DeletableUserType.IDP);

        // then
        assertTrue(result);
    }
    @Test
    void shouldBeDeletableGivenUserHasFederationLinkButNoIdpAlias() {
        // given
        UserModel user = mock(UserModel.class);
        when(user.getFederationLink()).thenReturn("federationLink");

        // when
        boolean result = UserDeletionChecker.userShouldBeDeleted(user, DeletableUserType.IDP);

        // then
        assertTrue(result);
    }
    @Test
    void shouleBeDeletableGivenAllUsersAreDeletableByConfiguration() {
        // given
        UserModel user = mock(UserModel.class);

        // when
        boolean result = UserDeletionChecker.userShouldBeDeleted(user, DeletableUserType.ALL);

        // then
        assertTrue(result);
    }

    @Test
    void shouldNotBeDeletable_whenUserNotExists() {
        // given
        UserModel user = null;

        // when
        boolean result = UserDeletionChecker.userShouldBeDeleted(user, DeletableUserType.IDP);

        // then
        assertFalse(result);
    }

}