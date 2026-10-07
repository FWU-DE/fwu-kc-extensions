package de.intension.resources.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VidisAdminRealmResourceProviderTest {

    @Mock
    private KeycloakSession session;
    @Mock
    private Config.Scope config;
    @Mock
    private EntityManager em;
    @Mock
    private Query query;
    @Mock
    private RealmModel realm;
    @Mock
    private UserSessionProvider sessionProvider;
    @Mock
    private UserProvider userProvider;

    private VidisAdminRealmResourceProvider provider;

    @BeforeEach
    void setup() {
        JpaConnectionProvider jpa = mock(JpaConnectionProvider.class);
        when(session.getProvider(JpaConnectionProvider.class)).thenReturn(jpa);
        when(jpa.getEntityManager()).thenReturn(em);
        KeycloakContext context = mock(KeycloakContext.class);
        when(session.getContext()).thenReturn(context);
        when(context.getRealm()).thenReturn(realm);
        when(realm.getName()).thenReturn("fwu");
        when(realm.getId()).thenReturn("realm-id");
        when(session.sessions()).thenReturn(sessionProvider);
        when(session.users()).thenReturn(userProvider);
        when(em.createNativeQuery(anyString(), eq(UserEntity.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(config.get("fwu", DeletableUserType.NONE.name())).thenReturn(DeletableUserType.IDP.name());
        when(config.getInt(VidisAdminRealmResourceProvider.DELETION_TOLERANCE_CONFIG, 30)).thenReturn(30);

        AdminPermissionEvaluator auth = mock(AdminPermissionEvaluator.class);
        when(auth.users()).thenReturn(mock(UserPermissionEvaluator.class));
        provider = new VidisAdminRealmResourceProvider(session, config);
        provider.getResource(session, realm, auth, null);
    }

    @Test
    void shouldPageByUserIdAndSkipUsersWithSession() {
        when(query.getResultList()).thenReturn(List.of(user("a"), user("b")), List.of(user("c")), List.of());
        usersWithSession("a", "c");

        Response response = provider.deleteUsers(1000);

        assertThat(deletedUsers(response)).isEqualTo(1);
        ArgumentCaptor<UserModel> removed = ArgumentCaptor.forClass(UserModel.class);
        verify(userProvider).removeUser(eq(realm), removed.capture());
        assertThat(removed.getValue().getId()).isEqualTo("b");
        ArgumentCaptor<Object> lastUserIds = ArgumentCaptor.forClass(Object.class);
        verify(query, times(3)).setParameter(eq("lastUserId"), lastUserIds.capture());
        assertThat(lastUserIds.getAllValues()).as("cursor must move past users that were kept").containsExactly("", "b", "c");
    }

    @Test
    void shouldOnlySelectUsersCreatedBeforeTolerance() {
        when(config.getInt(VidisAdminRealmResourceProvider.DELETION_TOLERANCE_CONFIG, 30)).thenReturn(60);
        when(query.getResultList()).thenReturn(List.of());

        long before = System.currentTimeMillis();
        provider.deleteUsers(1000);
        long after = System.currentTimeMillis();

        ArgumentCaptor<Object> createdBefore = ArgumentCaptor.forClass(Object.class);
        verify(query).setParameter(eq("createdBefore"), createdBefore.capture());
        assertThat((Long) createdBefore.getValue()).isBetween(before - 60_000L, after - 60_000L);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(em).createNativeQuery(sql.capture(), eq(UserEntity.class));
        assertThat(sql.getValue()).contains("ue.created_timestamp <= :createdBefore", "federated_identity");
    }

    @Test
    void shouldNotRestrictToIdpUsers_whenAllConfigured() {
        when(config.get("fwu", DeletableUserType.NONE.name())).thenReturn(DeletableUserType.ALL.name());
        when(query.getResultList()).thenReturn(List.of());

        provider.deleteUsers(1000);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(em).createNativeQuery(sql.capture(), eq(UserEntity.class));
        assertThat(sql.getValue()).doesNotContain("federated_identity");
    }

    @Test
    void shouldStopAtMax() {
        when(query.getResultList()).thenReturn(List.of(user("a"), user("b")), List.of(user("c")));
        usersWithSession();

        Response response = provider.deleteUsers(2);

        assertThat(deletedUsers(response)).isEqualTo(2);
        verify(query).getResultList();
        verify(query).setParameter("chunkSize", 2);
    }

    @Test
    void shouldForbidDeletion_whenNoneConfigured() {
        when(config.get("fwu", DeletableUserType.NONE.name())).thenReturn(DeletableUserType.NONE.name());

        Response response = provider.deleteUsers(1000);

        assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
        verifyNoInteractions(em);
    }

    private void usersWithSession(String... userIds) {
        Set<String> ids = Set.of(userIds);
        when(sessionProvider.getUserSessionsStream(eq(realm), any(UserModel.class)))
                .thenAnswer(inv -> ids.contains(inv.<UserModel>getArgument(1).getId())
                        ? Stream.of(mock(UserSessionModel.class))
                        : Stream.empty());
    }

    private static UserEntity user(String id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setCreatedTimestamp(0L);
        return user;
    }

    private static int deletedUsers(Response response) {
        return ((UserDeletionResponse) response.getEntity()).getDeletedUsers();
    }
}
