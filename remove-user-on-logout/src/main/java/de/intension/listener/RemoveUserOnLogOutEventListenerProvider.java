package de.intension.listener;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaKeycloakTransaction;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import de.intension.resources.admin.DeletableUserType;
import de.intension.rest.LicenseConnectRestClient;
import de.intension.rest.model.RemoveLicenseRequest;
import de.intension.spi.RestClientProvider;
import jakarta.persistence.EntityManager;

/**
 * Event listener to remove user on logout for the users from identity providers.
 */
public class RemoveUserOnLogOutEventListenerProvider
    implements EventListenerProvider
{

    private static final Logger            LOG             = Logger.getLogger(RemoveUserOnLogOutEventListenerProvider.class);

    public static final String             LICENSE_URL     = "license-url";
    public static final String             LICENSE_API_KEY = "license-api-key";

    private final KeycloakSession          keycloakSession;

    private final EventListenerTransaction tx              = new EventListenerTransaction(null, this::removeUser);
    private final Config.Scope             config;
    private LicenseConnectRestClient       restClient;

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
     * Starts a jpa transaction before removing the user since eventlistener tarnsaction does not
     * hava a
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
            this.releaseLicenses(userToDelete, event);
            userProvider.removeUser(realm, userToDelete);
            LOG.infof("User %s removed.", userToDelete.getUsername());
        }

        transaction.commit();
    }

    private UserModel findUserForDeletion(KeycloakSession keycloakSession, String userId)
    {
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

    private void releaseLicenses(UserModel user, Event event)
    {
        this.restClient = this.keycloakSession.getProvider(RestClientProvider.class).restClient();
        RemoveLicenseRequest licenseRequest = createLicenseReleaseRequest(user, event);
        boolean licenseReleased = false;
        try {
            if (this.restClient != null) {
                licenseReleased = this.restClient.releaseLicense(licenseRequest);
            }
        } catch (Exception e) {
            LOG.warn(e.getLocalizedMessage());
        }
        if (licenseReleased) {
            LOG.infof("User license has been released for the user %s", user.getUsername());
        }
        else {
            LOG.warnf("User license not released for the user %s", user.getUsername());
        }
    }

    private RemoveLicenseRequest createLicenseReleaseRequest(UserModel user, Event event)
    {
        RemoveLicenseRequest licenseRequestedRequest = null;
        RealmModel realm = this.keycloakSession.realms().getRealm(event.getRealmId());

        Set<String> idps = realm.getIdentityProvidersStream().map(IdentityProviderModel::getAlias).collect(Collectors.toSet());
        Stream<FederatedIdentityModel> federatedIdentityModelList = keycloakSession.users().getFederatedIdentitiesStream(realm, user)
            .filter(identity -> idps.contains(identity.getIdentityProvider()));

        Optional<FederatedIdentityModel> idp = federatedIdentityModelList.findFirst();
        if (idp.isPresent()) {
            String userId = idp.get().getUserId();
            licenseRequestedRequest = new RemoveLicenseRequest(userId);
        }
        return licenseRequestedRequest;
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

    public LicenseConnectRestClient getRestClient()
    {
        return this.restClient;
    }
}
