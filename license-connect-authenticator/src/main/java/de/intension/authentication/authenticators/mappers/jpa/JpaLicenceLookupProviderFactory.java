package de.intension.authentication.authenticators.mappers.jpa;

import de.intension.authentication.authenticators.mappers.LicenceLookupProvider;
import de.intension.authentication.authenticators.mappers.LicenceLookupProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class JpaLicenceLookupProviderFactory implements LicenceLookupProviderFactory {
    public static final String PROVIDER_ID = "jpa-licenceLookup";

    @Override
    public LicenceLookupProvider create(KeycloakSession keycloakSession) {
        return new JpaLicenceLookupProvider();
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
        //Nothing to do
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
