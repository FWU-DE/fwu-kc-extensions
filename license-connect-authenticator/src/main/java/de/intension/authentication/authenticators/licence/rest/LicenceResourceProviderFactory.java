package de.intension.authentication.authenticators.licence.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

public class LicenceResourceProviderFactory implements RealmResourceProviderFactory {

    private String realmName;

    @Override
    public RealmResourceProvider create(KeycloakSession keycloakSession) {
        return new LicenceResourceProvider(keycloakSession, realmName);
    }

    @Override
    public void init(Config.Scope scope) {
        realmName = scope.get("REALM_NAME");
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
