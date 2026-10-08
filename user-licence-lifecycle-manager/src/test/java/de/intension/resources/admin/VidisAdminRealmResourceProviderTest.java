package de.intension.resources.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.Query;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
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

    private MockedStatic<KeycloakModelUtils> modelUtils;
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
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(em.find(eq(UserEntity.class), anyString(), eq(LockModeType.PESSIMISTIC_WRITE), anyMap()))
                .thenAnswer(inv -> user(inv.getArgument(1)));
        when(config.get("fwu", DeletableUserType.NONE.name())).thenReturn(DeletableUserType.IDP.name());
        when(config.getInt(VidisAdminRealmResourceProvider.DELETION_TOLERANCE_CONFIG, 30)).thenReturn(30);
        when(config.getInt(VidisAdminRealmResourceProvider.MAX_RUNTIME_CONFIG, 60)).thenReturn(60);

        // run the jobs directly with the mocked session instead of opening new sessions and transactions
        modelUtils = mockStatic(KeycloakModelUtils.class);
        modelUtils.when(() -> KeycloakModelUtils.runJobInTransactionWithResult(any(), any(), any(), anyString()))
                .thenAnswer(inv -> inv.<KeycloakSessionTaskWithResult<?>>getArgument(2).run(session));

        AdminPermissionEvaluator auth = mock(AdminPermissionEvaluator.class);
        when(auth.users()).thenReturn(mock(UserPermissionEvaluator.class));
        provider = new VidisAdminRealmResourceProvider(session, config);
        provider.getResource(session, realm, auth, null);
    }

    @AfterEach
    void tearDown() {
        modelUtils.close();
    }

    @Test
    void shouldPageByUserIdAndSkipUsersWithSession() {
        when(query.getResultList()).thenReturn(List.of("a", "b"), List.of("c"), List.of());
        usersWithSession("a", "c");

        Response response = provider.deleteUsers(1000);

        assertThat(deletedUsers(response)).isEqualTo(1);
        assertThat(removedUserIds()).containsExactly("b");
        ArgumentCaptor<Object> lastUserIds = ArgumentCaptor.forClass(Object.class);
        verify(query, times(3)).setParameter(eq("lastUserId"), lastUserIds.capture());
        assertThat(lastUserIds.getAllValues()).as("cursor must move past users that were kept").containsExactly("", "b", "c");
    }

    @Test
    void shouldSkipUser_whenLockFails() {
        when(query.getResultList()).thenReturn(List.of("a", "b"), List.of());
        usersWithSession();
        when(em.find(eq(UserEntity.class), eq("a"), eq(LockModeType.PESSIMISTIC_WRITE), anyMap()))
                .thenThrow(new PessimisticLockException("locked"));

        Response response = provider.deleteUsers(1000);

        assertThat(deletedUsers(response)).isEqualTo(1);
        assertThat(removedUserIds()).containsExactly("b");
    }

    @Test
    void shouldSkipUser_whenAlreadyDeleted() {
        when(query.getResultList()).thenReturn(List.of("a"), List.of());
        when(em.find(eq(UserEntity.class), eq("a"), eq(LockModeType.PESSIMISTIC_WRITE), anyMap())).thenReturn(null);

        Response response = provider.deleteUsers(1000);

        assertThat(deletedUsers(response)).isZero();
        verify(userProvider, never()).removeUser(any(), any());
    }

    @Test
    void shouldStop_whenMaxRuntimeReached() {
        when(config.getInt(VidisAdminRealmResourceProvider.MAX_RUNTIME_CONFIG, 60)).thenReturn(0);
        when(query.getResultList()).thenReturn(List.of("a"));

        Response response = provider.deleteUsers(1000);

        assertThat(deletedUsers(response)).isZero();
        verify(userProvider, never()).removeUser(any(), any());
    }

    @Test
    void shouldOnlySelectUsersCreatedBeforeToleranceAndWithoutPersistedSession() {
        when(config.getInt(VidisAdminRealmResourceProvider.DELETION_TOLERANCE_CONFIG, 30)).thenReturn(60);
        when(query.getResultList()).thenReturn(List.of());

        long before = System.currentTimeMillis();
        provider.deleteUsers(1000);
        long after = System.currentTimeMillis();

        ArgumentCaptor<Object> createdBefore = ArgumentCaptor.forClass(Object.class);
        verify(query).setParameter(eq("createdBefore"), createdBefore.capture());
        assertThat((Long) createdBefore.getValue()).isBetween(before - 60_000L, after - 60_000L);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(em).createNativeQuery(sql.capture());
        assertThat(sql.getValue()).contains("ue.created_timestamp <= :createdBefore", "offline_user_session", "federated_identity");
    }

    @Test
    void shouldNotRestrictToIdpUsers_whenAllConfigured() {
        when(config.get("fwu", DeletableUserType.NONE.name())).thenReturn(DeletableUserType.ALL.name());
        when(query.getResultList()).thenReturn(List.of());

        provider.deleteUsers(1000);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(em).createNativeQuery(sql.capture());
        assertThat(sql.getValue()).doesNotContain("federated_identity");
    }

    @Test
    void shouldStopAtMax() {
        when(query.getResultList()).thenReturn(List.of("a", "b"), List.of("c"));
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

    private List<String> removedUserIds() {
        ArgumentCaptor<UserModel> removed = ArgumentCaptor.forClass(UserModel.class);
        verify(userProvider, atLeast(0)).removeUser(eq(realm), removed.capture());
        return removed.getAllValues().stream().map(UserModel::getId).toList();
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
