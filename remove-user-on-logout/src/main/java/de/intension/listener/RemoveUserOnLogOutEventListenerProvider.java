package de.intension.listener;

import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaKeycloakTransaction;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

/**
 * Event listener to remove user on logout for the users from identity providers.
 */
public class RemoveUserOnLogOutEventListenerProvider
        implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(RemoveUserOnLogOutEventListenerProvider.class);

    private final KeycloakSession keycloakSession;

    private final EventListenerTransaction tx = new EventListenerTransaction(null, this::removeUser);

    protected RemoveUserOnLogOutEventListenerProvider(KeycloakSession session) {
        this.keycloakSession = session;
        session.getTransactionManager().enlistAfterCompletion(tx);
        LOG.debugf("[%s] instantiated.", this.getClass());
    }

    @Override
    public void onEvent(Event event) {
        EventType eventType = event.getType();

        if (EventType.LOGOUT.equals(eventType)) {
            tx.addEvent(event);
        }
    }

    /**
     * Remove user from keycloak on logout.
     * Starts a jpa transaction before removing the user since eventlistener tarnsaction does not hava a running jpa-transaction
     */
    private void removeUser(Event event) {
        EntityManager entityManager = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
        JpaKeycloakTransaction transaction = new JpaKeycloakTransaction(entityManager);
        transaction.begin();
        RealmModel realm = keycloakSession.getContext().getRealm();
        UserProvider userProvider = keycloakSession.users();

        UserModel user = userProvider.getUserById(realm, event.getUserId());
        if (user != null) {
            userProvider.removeUser(realm, user);
            LOG.debugf("[%s] User %s removed.", this.getClass(), user.getUsername());
        }
        transaction.commit();
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // no action on admin events.
    }

    @Override
    public void close() {
        // nothing to close.
    }
}
