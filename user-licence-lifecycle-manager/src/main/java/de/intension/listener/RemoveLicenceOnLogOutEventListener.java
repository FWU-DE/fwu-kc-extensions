package de.intension.listener;

import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.UserRemovedEvent;
import org.keycloak.models.UserProvider;
import org.keycloak.provider.ProviderEvent;
import org.keycloak.provider.ProviderEventListener;

/**
 * Event listener to remove user licence on logout.
 * <hr>
 * This is an implementation of {@link ProviderEventListener} which reacts to the delete user event
 * created by {@link org.keycloak.models.UserManager#removeUser(RealmModel, UserModel, UserProvider)} in {@link RemoveUserOnLogOutEventListenerProvider#removeUser(Event)}.
 */
public class RemoveLicenceOnLogOutEventListener implements ProviderEventListener {

    private static final Logger LOG = Logger.getLogger(RemoveLicenceOnLogOutEventListener.class);

    private final KeycloakSession keycloakSession;

    public RemoveLicenceOnLogOutEventListener(KeycloakSession keycloakSession) {
        this.keycloakSession = keycloakSession;
    }

    @Override
    public void onEvent(ProviderEvent event) {
        if (event instanceof UserRemovedEvent userRemovedEvent) {
            UserModel user = userRemovedEvent.getUser();
            deleteLicence(user);
        }
    }

    private void deleteLicence(UserModel user) {
        var hmacMapper = keycloakSession.getContext().getClient().getProtocolMappersStream()
                .filter(mapper -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(mapper.getProtocolMapper())).findFirst();
        if (hmacMapper.isPresent()) {
            String hmacId = HmacPairwiseSubMapperHelper.generateIdentifier(hmacMapper.get(), user);
            keycloakSession.getProvider(LicenceJpaProvider.class).deleteLicence(hmacId);
            LOG.infof("User licence has been removed from the database for user %s", user.getUsername());
        }
    }
}
