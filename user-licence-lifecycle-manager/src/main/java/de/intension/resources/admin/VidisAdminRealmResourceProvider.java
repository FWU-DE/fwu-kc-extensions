package de.intension.resources.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.time.StopWatch;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.*;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.UserPermissionEvaluator;

import java.util.List;
import java.util.Map;

public class VidisAdminRealmResourceProvider
        implements AdminRealmResourceProvider {

    private static final Logger LOG = Logger.getLogger(VidisAdminRealmResourceProvider.class);
    private static final int DEFAULT_TOLERANCE_FOR_USER_IN_CREATION_IN_SECONDS = 30;
    private static final int DEFAULT_MAX_RUNTIME_IN_SECONDS = 60;
    private static final String LOCK_TIMEOUT_HINT = "jakarta.persistence.lock.timeout";
    public static final String DELETION_TOLERANCE_CONFIG = "deletiontolerance";
    public static final String MAX_RUNTIME_CONFIG = "maxruntime";

    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private final Config.Scope config;

    public VidisAdminRealmResourceProvider(KeycloakSession session, Config.Scope config) {
        this.session = session;
        this.config = config;
    }

    @Override
    public void close() {
        //nothing to do
    }

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        return this;
    }

    @DELETE
    @Path("users/inactive")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUsers(@QueryParam("max") @DefaultValue("1000") Integer max) {
        UserPermissionEvaluator userPermissionEvaluator = auth.users();
        userPermissionEvaluator.requireQuery();
        DeletableUserType deletableUserType = DeletableUserType
                .valueOf(config.get(session.getContext().getRealm().getName().toLowerCase(), DeletableUserType.NONE.name()));

        String realm = session.getContext().getRealm().getName();
        if (realm.equals("master") || deletableUserType == DeletableUserType.NONE) {
            LOG.info("User deletion is not allowed for realm " + realm);
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        StopWatch watch = new StopWatch();
        watch.start();
        int amountOfDeletedUsers = deleteUsersWithoutSession(Math.min(max, 1000), deletableUserType.equals(DeletableUserType.IDP));
        watch.stop();
        LOG.infof("%s users were cleaned up in %s ms", amountOfDeletedUsers, watch.getTime());
        return Response.ok().type(MediaType.APPLICATION_JSON).entity(new UserDeletionResponse(amountOfDeletedUsers)).build();
    }

    private int deleteUsersWithoutSession(int maxNoOfUserToDelete, boolean idpOnly) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        KeycloakContext context = session.getContext();
        int numberOfDeletedUsers = 0;
        // users created after the cutoff may still be in the middle of their login and do not have a session yet
        long createdBefore = System.currentTimeMillis()
                - (long) config.getInt(DELETION_TOLERANCE_CONFIG, DEFAULT_TOLERANCE_FOR_USER_IN_CREATION_IN_SECONDS) * 1000L;
        // stop in time, so a run never overlaps with the next scheduled one
        long deadline = System.currentTimeMillis() + config.getInt(MAX_RUNTIME_CONFIG, DEFAULT_MAX_RUNTIME_IN_SECONDS) * 1000L;
        // users with an active session are not deleted, so page by id to not fetch them again
        String lastUserId = "";
        while (numberOfDeletedUsers < maxNoOfUserToDelete && System.currentTimeMillis() < deadline) {
            String cursor = lastUserId;
            int chunkSize = Math.min(250, maxNoOfUserToDelete - numberOfDeletedUsers);
            List<String> userIds = KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, context,
                    s -> getUserIds(s, chunkSize, cursor, createdBefore, idpOnly), "vidis user cleanup scan");
            LOG.debugf("Found %s users in realm %s", userIds.size(), context.getRealm().getName());
            if (userIds.isEmpty()) {
                break;
            }
            for (String userId : userIds) {
                if (System.currentTimeMillis() >= deadline) {
                    LOG.infof("User cleanup stopped after reaching max runtime");
                    break;
                }
                // every user is deleted in its own short transaction, so a failure does not roll back the whole run
                try {
                    if (KeycloakModelUtils.runJobInTransactionWithResult(sessionFactory, context,
                            s -> deleteUserWithoutSession(s, userId), "vidis user cleanup delete")) {
                        numberOfDeletedUsers++;
                    }
                } catch (RuntimeException e) {
                    LOG.warnf("Could not delete user %s, skipping: %s", userId, e.getMessage());
                }
            }
            lastUserId = userIds.get(userIds.size() - 1);
        }
        return numberOfDeletedUsers;
    }

    private boolean deleteUserWithoutSession(KeycloakSession s, String userId) {
        RealmModel realm = s.getContext().getRealm();
        EntityManager em = s.getProvider(JpaConnectionProvider.class).getEntityManager();
        // a user locked by a login or a concurrent cleanup run fails immediately and is skipped instead of blocking
        UserEntity ue = em.find(UserEntity.class, userId, LockModeType.PESSIMISTIC_WRITE, Map.of(LOCK_TIMEOUT_HINT, 0));
        if (ue == null) {
            return false;
        }
        UserAdapter user = new UserAdapter(s, realm, em, ue);
        if (s.sessions().getUserSessionsStream(realm, user).findAny().isPresent()) {
            return false;
        }
        s.users().removeUser(realm, user);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getUserIds(KeycloakSession s, int chunkSize, String lastUserId, long createdBefore, boolean idpOnly) {
        EntityManager em = s.getProvider(JpaConnectionProvider.class).getEntityManager();
        String idpOnlyClause = idpOnly ? " and exists (select 1 from federated_identity fi where fi.user_id = ue.id) "
                : " ";
        // pre-filter on persisted online sessions, the authoritative session check is done before deletion
        Query userQuery = em.createNativeQuery("select ue.id "
                + "from user_entity ue "
                + "where ue.realm_id = :realmId "
                + "and ue.id > :lastUserId "
                + "and (ue.created_timestamp <= :createdBefore or ue.created_timestamp is null) "
                + "and not exists (select 1 from offline_user_session us "
                + "where us.user_id = ue.id and us.realm_id = ue.realm_id and us.offline_flag = '0') "
                + idpOnlyClause
                + "order by ue.id asc "
                + "LIMIT :chunkSize");
        userQuery.setParameter("lastUserId", lastUserId);
        userQuery.setParameter("createdBefore", createdBefore);
        userQuery.setParameter("chunkSize", chunkSize);
        userQuery.setParameter("realmId", s.getContext().getRealm().getId());
        return userQuery.getResultList();
    }
}
