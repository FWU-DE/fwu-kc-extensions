package de.intension.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class LicenceResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public LicenceResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new LicenceResource(session);
    }

    @Override
    public void close() {

    }
}
