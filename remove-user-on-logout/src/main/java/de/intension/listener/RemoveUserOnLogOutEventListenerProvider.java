package de.intension.listener;

import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.timer.TimerProvider;

import de.intension.task.RemoveExpiredSessionUsers;

/**
 * Event listener to remove user on logout for the users from identity providers.
 */
public class RemoveUserOnLogOutEventListenerProvider
    implements EventListenerProvider
{

    private static final Logger   LOG = Logger.getLogger(RemoveUserOnLogOutEventListenerProvider.class);

    private final KeycloakSession keycloakSession;

    protected RemoveUserOnLogOutEventListenerProvider(KeycloakSession session)
    {
        this.keycloakSession = session;
        LOG.debugf("[%s] instantiated.", this.getClass());
    }

    @Override
    public void onEvent(Event event)
    {
        EventType eventType = event.getType();

        if (EventType.LOGIN.equals(eventType) || EventType.LOGOUT.equals(eventType) || EventType.REGISTER.equals(eventType)) {
            RealmModel realm = keycloakSession.getContext().getRealm();
            String userId = event.getUserId();
            UserProvider provider = this.keycloakSession.getProvider(UserProvider.class, "jpa");
            UserModel userModel = provider.getUserById(realm, userId);
            Stream<FederatedIdentityModel> federatedIdentity = keycloakSession.users()
                .getFederatedIdentitiesStream(realm, userModel);

            if (federatedIdentity.findAny().isPresent()) {
                if (EventType.LOGOUT.equals(eventType)) {
                    removeUserAndCancelTask(realm, userModel, userId);
                }
                else {
                    addUserRemovalTask(realm, event, userModel, userId, EventType.LOGIN.equals(eventType));
                }
            }
        }
    }

    /**
     * Add user removal task.
     */
    private void addUserRemovalTask(RealmModel realm, Event event, UserModel userModel, String userId, boolean removeSession)
    {
        TimerProvider timer = keycloakSession.getProvider(TimerProvider.class);
        if (removeSession) {
            // keep one session per user
            keycloakSession.sessions().getUserSessionsStream(realm, userModel).forEach(userSession -> {
                if (!userSession.getId().equals(event.getSessionId())) {
                    keycloakSession.sessions().removeUserSession(realm, userSession);
                }
            });
        }

        timer.schedule(new ScheduledTaskRunner(KeycloakApplication.getSessionFactory(), new RemoveExpiredSessionUsers(realm, userId)),
                       realm.getSsoSessionIdleTimeout() * 1000L, userId);
        LOG.debugf("Task created to check and remove user with id [%s] on session expiration.", userId);
    }

    /**
     * Remove user by userId and cancel related 'remove expired session users' task.
     */
    private void removeUserAndCancelTask(RealmModel realm, UserModel userModel, String userId)
    {
        TimerProvider timer = keycloakSession.getProvider(TimerProvider.class);
        keycloakSession.users().removeUser(realm, userModel);
        keycloakSession.getTransactionManager().commit();
        if (timer.cancelTask(userId) != null) {
            LOG.debugf("Task with id [%s] is removed on logout.");
        }
        LOG.debugf("User with id [%s] is removed on logout.", userId);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation)
    {
        // no action on admin events.
    }

    @Override
    public void close()
    {
        // nothing to close.
    }
}
