package de.intension.task;

import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

/**
 * Scheduled task for removing the users of expired sessions.
 */
public class RemoveExpiredSessionUsers
    implements ScheduledTask
{

    private static final Logger LOG = Logger.getLogger(RemoveExpiredSessionUsers.class);

    private RealmModel          realm;

    private String              userId;

    public RemoveExpiredSessionUsers(RealmModel realm, String userId)
    {
        this.realm = realm;
        this.userId = userId;
    }

    @Override
    public void run(KeycloakSession session)
    {
        UserProvider provider = session.getProvider(UserProvider.class, "jpa");
        UserModel userModel = provider.getUserById(realm, userId);
        Stream<UserSessionModel> userSessionModels = session.sessions().getUserSessionsStream(realm, userModel);
        Optional<UserSessionModel> userSession = userSessionModels.findFirst();

        if (userModel != null && (!userSession.isPresent() || !AuthenticationManager.isSessionValid(realm, userSession.get()))) {
            removeUser(session, userModel);
        }
    }

    private void removeUser(KeycloakSession session, UserModel userModel)
    {
        TimerProvider timer = session.getProvider(TimerProvider.class);
        session.sessions().getUserSessionsStream(this.realm, userModel).forEach(userSessionModel -> {
            session.sessions().removeUserSession(realm, userSessionModel);
        });
        session.users().removeUser(this.realm, userModel);
        session.getTransactionManager().commit();
        // cancel the timer as it is not needed anymore
        timer.cancelTask(this.userId);
        LOG.debugf("User with id [%s] is removed since the session is expired.", userId);
    }

}
