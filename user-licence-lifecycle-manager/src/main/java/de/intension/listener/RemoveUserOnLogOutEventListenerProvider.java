package de.intension.listener;

import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
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
        try {
            KeycloakModelUtils.runJobInTransaction(keycloakSession.getKeycloakSessionFactory(), session -> {
                try {
                    LOG.debugf("Starting user deletion for userId=%s, realmId=%s, clientId=%s", 
                               event.getUserId(), event.getRealmId(), event.getClientId());
                    
                    RealmModel realm = session.getContext().getRealm();
                    if (realm == null) {
                        realm = session.realms().getRealm(event.getRealmId());
                        if (realm == null) {
                            LOG.warnf("Realm not found for realmId=%s, cannot delete user", event.getRealmId());
                            return;
                        }
                        session.getContext().setRealm(realm);
                    }
                    
                    UserManager userManager = new UserManager(session);

                    UserModel userToDelete = findUserForDeletion(session, event.getUserId());
                    if (userToDelete != null) {
                        String hmacId = null;
                        String username = userToDelete.getUsername();
                        
                        // Get client from the new session using clientId from event
                        var client = session.clients().getClientByClientId(realm, event.getClientId());
                        if (client != null) {
                            var hmacMapper = client.getProtocolMappersStream()
                                    .filter(mapper -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(mapper.getProtocolMapper())).findFirst();
                            if (hmacMapper.isPresent()) {
                                hmacId = HmacPairwiseSubMapperHelper.generateIdentifier(hmacMapper.get(), userToDelete);
                            }
                        } else {
                            LOG.warnf("Client not found for clientId=%s, proceeding without HMAC deletion", event.getClientId());
                        }
                        
                        LOG.debugf("Removing user %s from realm %s", username, realm.getName());
                        if (userManager.removeUser(realm, userToDelete)) {
                            LOG.infof("User %s removed.", userToDelete.getUsername());
                            if (hmacId != null) {
                                session.getProvider(LicenceJpaProvider.class).deleteLicence(hmacId);
                                LOG.infof("User licence has been removed from the database for user %s", username);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.errorf(e, "Failed to delete user in transaction for userId=%s, realmId=%s, clientId=%s", 
                               event.getUserId(), event.getRealmId(), event.getClientId());
                }
            });
        } catch (Exception e) {
            LOG.errorf(e, "Failed to start transaction for user deletion: userId=%s, realmId=%s, clientId=%s", 
                       event.getUserId(), event.getRealmId(), event.getClientId());
        }
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
