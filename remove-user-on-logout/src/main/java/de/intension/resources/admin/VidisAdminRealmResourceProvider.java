package de.intension.resources.admin;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
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

    private static final Logger      LOG = Logger.getLogger(VidisAdminRealmResourceProvider.class);

    private final KeycloakSession    session;
    private AdminPermissionEvaluator auth;

    public VidisAdminRealmResourceProvider(KeycloakSession session)
    {
        this.session = session;
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

        StopWatch watch = new StopWatch();
        watch.start();
        int amountOfDeletedUsers = deleteIdPUserWithoutSession(Math.min(max, 1000));
        watch.stop();
        LOG.infof("%s users were cleaned up in %s ms", amountOfDeletedUsers, watch.getTime());
        return Response.ok().type(MediaType.APPLICATION_JSON).build();
    }

    private int deleteIdPUserWithoutSession(int maxNoOfUserToDelete)
    {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        RealmModel realmModel = session.getContext().getRealm();
        UserSessionProvider usp = session.sessions();
        int numberOfDeletedUsers = 0;
        Long lastCreationDate = 0L;
        do {
            List<UserEntity> idpUsers = getListOfUserIdPUsers(Math.min(250, maxNoOfUserToDelete), lastCreationDate);
            if (!idpUsers.isEmpty()) {
                for (UserEntity ue : idpUsers) {
                    lastCreationDate = ue.getCreatedTimestamp();
                    if (lastCreationDate > Time.currentTimeMillis() - TimeUnit.SECONDS.toMillis(5)) {
                        continue;
                    }
                    UserAdapter ua = new UserAdapter(session, realmModel, em, ue);
                    if (usp.getUserSessionsStream(realmModel, ua).noneMatch(userSession -> true)) {
                        if(session.users().removeUser(realmModel, ua)){
                            numberOfDeletedUsers++;
                        }
                    }
                }
            }
            else {
                break;
            }
        } while (numberOfDeletedUsers < maxNoOfUserToDelete);
        return numberOfDeletedUsers;
    }

    private List<UserEntity> getListOfUserIdPUsers(int chunkSize, long lastCreationDate)
    {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        Query userQuery = em.createNativeQuery("select ue.* "
                + "from user_entity ue "
                + "where exists (select 1 from federated_identity fi where fi.user_id = ue.id) "
                + "and ue.created_timestamp > :lastTimeStamp "
                + "order by ue.created_timestamp asc "
                + "LIMIT :chunkSize", UserEntity.class);
        userQuery.setParameter("lastTimeStamp", lastCreationDate);
        userQuery.setParameter("chunkSize", chunkSize);
        return userQuery.getResultList();
    }

}
