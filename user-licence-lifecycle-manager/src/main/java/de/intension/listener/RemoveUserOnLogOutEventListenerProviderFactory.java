package de.intension.listener;

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

    private Scope config;

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new RemoveUserOnLogOutEventListenerProvider(session, config);
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
        factory.register(new RemoveLicenceOnLogOutEventListener(factory.create()));
    }

    @Override
    public void close() {
    }
}
