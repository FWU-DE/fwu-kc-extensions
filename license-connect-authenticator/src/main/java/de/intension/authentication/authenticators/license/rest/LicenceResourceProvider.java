package de.intension.authentication.authenticators.license.rest;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class LicenceResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;
    private final String realmName;

    public LicenceResourceProvider(KeycloakSession session, String realmName) {
        this.session = session;
        this.realmName = realmName;
    }

    @Override
    public Object getResource() {
        return new LicenceResource(session, realmName);
    }

    @Override
    public void close() {

    }
}
