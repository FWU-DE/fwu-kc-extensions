package de.intension.authentication.authenticators.backup.jpa.entity;

import org.keycloak.Config;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class LicenceEntityProviderFactory implements JpaEntityProviderFactory {
    protected static final String ID = "pseudonym-licence-mapping-entity-provider";

    @Override
    public JpaEntityProvider create(KeycloakSession keycloakSession) {
        return new LicenceEntityProvider();
    }

    @Override
    public void init(Config.Scope scope) {
        //Nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
        // Nothing to do
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public String getId() {
        return ID;
    }
}
