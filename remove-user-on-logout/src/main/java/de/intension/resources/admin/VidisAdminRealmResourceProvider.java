package de.intension.resources.admin;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.time.StopWatch;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.jpa.UserAdapter;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.UserPermissionEvaluator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class VidisAdminRealmResourceProvider
    implements AdminRealmResourceProvider
{

    private static final Logger      LOG                                               = Logger.getLogger(VidisAdminRealmResourceProvider.class);
    private static final int         DEFAULT_TOLERANCE_FOR_USER_IN_CREATION_IN_SECONDS = 30;
    public static final String       DELETION_TOLERANCE_CONFIG                         = "deletiontolerance";

    private final KeycloakSession    session;
    private AdminPermissionEvaluator auth;
    private Config.Scope             config;

    public VidisAdminRealmResourceProvider(KeycloakSession session, Config.Scope config)
    {
        this.session = session;
        this.config = config;
    }

    @Override
    public void close()
    {
        //nothing to do
    }

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent)
    {
        this.auth = auth;
        return this;
    }

    @DELETE
    @Path("users/inactive")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUsers(@QueryParam("max") @DefaultValue("1000") Integer max)
    {
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

    private int deleteUsersWithoutSession(int maxNoOfUserToDelete, boolean idpOnly)
    {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        RealmModel realmModel = session.getContext().getRealm();
        UserSessionProvider sessionProvider = session.sessions();
        int numberOfDeletedUsers = 0;
        Long lastCreationDate = new Date().toInstant().getEpochSecond()
                - config.getInt(DELETION_TOLERANCE_CONFIG, DEFAULT_TOLERANCE_FOR_USER_IN_CREATION_IN_SECONDS);
        do {
            List<UserEntity> idpUsers = getListOfUsers(Math.min(250, maxNoOfUserToDelete), lastCreationDate, idpOnly);
            LOG.debugf("Found %s users in realm %s", idpUsers.size(), realmModel.getName());
            if (idpUsers.isEmpty()) {
                break;
            }
            for (UserEntity ue : idpUsers) {
                lastCreationDate = ue.getCreatedTimestamp() != null ? ue.getCreatedTimestamp() : lastCreationDate;
                UserAdapter ua = new UserAdapter(session, realmModel, em, ue);
                if (sessionProvider.getUserSessionsStream(realmModel, ua).noneMatch(userSession -> true) && session.users().removeUser(realmModel, ua)) {
                    numberOfDeletedUsers++;
                }

            }
        } while (numberOfDeletedUsers < maxNoOfUserToDelete);
        return numberOfDeletedUsers;
    }

    private List<UserEntity> getListOfUsers(int chunkSize, long lastCreationDate, boolean idpOnly)
    {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        String idpOnlyClause = idpOnly ? " and exists (select 1 from federated_identity fi where fi.user_id = ue.id) "
                : " ";
        Query userQuery = em.createNativeQuery("select ue.* "
                + "from user_entity ue "
                + "where ue.created_timestamp > :lastTimeStamp "
                + "and ue.realm_id = :realmId "
                + idpOnlyClause
                + "order by ue.created_timestamp asc "
                + "LIMIT :chunkSize", UserEntity.class);
        userQuery.setParameter("lastTimeStamp", lastCreationDate);
        userQuery.setParameter("chunkSize", chunkSize);
        userQuery.setParameter("realmId", session.getContext().getRealm().getId());
        return userQuery.getResultList();
    }
}
