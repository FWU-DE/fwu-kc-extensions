package de.intension.rest;

import lombok.RequiredArgsConstructor;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Provider and provider factory for {@link LicenceResource}.
 */
@RequiredArgsConstructor
public class LicenceResourceProviderFactory implements RealmResourceProvider, RealmResourceProviderFactory {

    private final KeycloakSession session;

    @Override
    public RealmResourceProvider create(KeycloakSession keycloakSession) {
        return this;
    }

    @Override
    public Object getResource() {
        return new LicenceResource(session);
    }

    @Override
    public void init(Config.Scope scope) {
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "licences-resource";
    }
}
