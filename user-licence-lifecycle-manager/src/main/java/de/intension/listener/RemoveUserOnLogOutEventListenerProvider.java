package de.intension.listener;

import de.intension.resources.admin.DeletableUserType;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Event listener to remove user on logout for the users from identity providers.
 */
public class RemoveUserOnLogOutEventListenerProvider
        implements EventListenerProvider {

    private static final Logger LOG = Logger.getLogger(RemoveUserOnLogOutEventListenerProvider.class);

    private final KeycloakSession keycloakSession;

    private final EventListenerTransaction tx = new EventListenerTransaction(null, this::removeUser);
    private final Config.Scope config;

    protected RemoveUserOnLogOutEventListenerProvider(KeycloakSession session, Config.Scope config) {
        this.keycloakSession = session;
        this.config = config;
        session.getTransactionManager().enlistAfterCompletion(tx);
        LOG.debugf("[%s] instantiated.", this.getClass());
    }

    @Override
    public void onEvent(Event event) {
        EventType eventType = event.getType();

        if (EventType.LOGOUT.equals(eventType) && !"master".equals(keycloakSession.getContext().getRealm().getName())) {
            tx.addEvent(event);
        }
    }

    /**
     * Remove user from keycloak on logout.
     * Starts a jpa transaction before removing the user since eventlistener tarnsaction does not
     * hava a running jpa-transaction
     */
    private void removeUser(Event event) {
        KeycloakModelUtils.runJobInTransaction(keycloakSession.getKeycloakSessionFactory(), session -> {
            RealmModel realm = session.getContext().getRealm();
            if (realm == null) {
                realm = session.realms().getRealm(event.getRealmId());
                session.getContext().setRealm(realm);
            }
            if (session.getContext().getClient() == null) {
                // needed for RemoveLicenceOnLogOutEventListener
                session.getContext().setClient(keycloakSession.getContext().getClient());
            }
            UserManager userManager = new UserManager(session);

            UserModel userToDelete = findUserForDeletion(session, event.getUserId());
            if (userToDelete != null) {
                userManager.removeUser(realm, userToDelete);
                LOG.infof("User %s removed.", userToDelete.getUsername());
            }
        });
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
        if (deletableUserType == DeletableUserType.ALL) {
            return user;
        }
        return keycloakSession.users().getFederatedIdentitiesStream(realm, user).findAny().isPresent() ? user : null;
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
