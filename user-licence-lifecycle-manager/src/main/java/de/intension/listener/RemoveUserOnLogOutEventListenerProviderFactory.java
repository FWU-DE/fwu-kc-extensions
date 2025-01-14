package de.intension.listener;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for the remove user on logout event listener.
 */
public class RemoveUserOnLogOutEventListenerProviderFactory
        implements EventListenerProviderFactory {

    private static final Logger LOG = Logger.getLogger(RemoveUserOnLogOutEventListenerProviderFactory.class);
    private RemoveUserOnLogOutEventListenerProvider removeUserOnLogOutEventListenerProvider;
    private Scope config;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        removeUserOnLogOutEventListenerProvider = new RemoveUserOnLogOutEventListenerProvider(session, config);
        return removeUserOnLogOutEventListenerProvider;
    }

    @Override
    public String getId() {
        return "remove-user-on-logout";
    }

    @Override
    public void init(Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nothing to do
    }

    @Override
    public void close() {
    }

}
