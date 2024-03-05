package de.intension.listener;

import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.*;

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

        if (EventType.LOGOUT.equals(eventType)) {
            RealmModel realm = keycloakSession.getContext().getRealm();
            String userId = event.getUserId();
            UserProvider provider = this.keycloakSession.getProvider(UserProvider.class, "jpa");
            UserModel userModel = provider.getUserById(realm, userId);
            Stream<FederatedIdentityModel> federatedIdentity = keycloakSession.users()
                .getFederatedIdentitiesStream(realm, userModel);
            if (federatedIdentity.findAny().isPresent()){
                removeUser(realm, userModel, userId);
            }
        }
    }

    /**
     * Remove user by userId and cancel related 'remove expired session users' task.
     */
    private void removeUser(RealmModel realm, UserModel userModel, String userId)
    {
        keycloakSession.users().removeUser(realm, userModel);
        keycloakSession.getTransactionManager().commit();
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
