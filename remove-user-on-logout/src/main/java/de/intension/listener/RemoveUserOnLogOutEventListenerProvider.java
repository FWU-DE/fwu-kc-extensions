package de.intension.listener;

import org.jboss.logging.Logger;
import org.keycloak.Config;
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

import de.intension.resources.admin.DeletableUserType;
import jakarta.persistence.EntityManager;

/**
 * Event listener to remove user on logout for the users from identity providers.
 */
public class RemoveUserOnLogOutEventListenerProvider
    implements EventListenerProvider
{

    private static final Logger            LOG = Logger.getLogger(RemoveUserOnLogOutEventListenerProvider.class);

    private final KeycloakSession          keycloakSession;

    private final EventListenerTransaction tx  = new EventListenerTransaction(null, this::removeUser);
    private final Config.Scope             config;

    protected RemoveUserOnLogOutEventListenerProvider(KeycloakSession session, Config.Scope config)
    {
        this.keycloakSession = session;
        this.config = config;
        session.getTransactionManager().enlistAfterCompletion(tx);
        LOG.debugf("[%s] instantiated.", this.getClass());
    }

    @Override
    public void onEvent(Event event)
    {
        EventType eventType = event.getType();

        if (EventType.LOGOUT.equals(eventType) && !"master".equals(keycloakSession.getContext().getRealm().getName())) {
            tx.addEvent(event);
        }
    }

    /**
     * Remove user from keycloak on logout.
     * Starts a jpa transaction before removing the user since eventlistener tarnsaction does not hava a
     * running jpa-transaction
     */
    private void removeUser(Event event)
    {
        EntityManager entityManager = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
        JpaKeycloakTransaction transaction = new JpaKeycloakTransaction(entityManager);
        transaction.begin();
        RealmModel realm = keycloakSession.getContext().getRealm();
        UserProvider userProvider = keycloakSession.getProvider(UserProvider.class, "jpa");

        UserModel userToDelete = findUserForDeletion(keycloakSession, event.getUserId());
        if (userToDelete != null) {
            userProvider.removeUser(realm, userToDelete);
            LOG.infof("User %s removed.", userToDelete.getUsername());
        }

        transaction.commit();
    }

    private UserModel findUserForDeletion(KeycloakSession keycloakSession, String userId) {
        RealmModel realm = keycloakSession.getContext().getRealm();
        DeletableUserType deletableUserType = DeletableUserType
                .valueOf(config.get(realm.getName().toLowerCase(), DeletableUserType.NONE.name()));
        if (deletableUserType == DeletableUserType.NONE || "master".equals(realm.getName())) {
            LOG.infof("Userdeletion for realm %s is disabled.", realm.getName());
            return null;
        }
        UserModel user = keycloakSession.users().getUserById(realm, userId);
        if (user == null) {
            return null;
        }
        if(deletableUserType == DeletableUserType.ALL) {
            return user;
        }
        return keycloakSession.users().getFederatedIdentitiesStream(realm, user).findAny().isPresent() ? user : null;
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
