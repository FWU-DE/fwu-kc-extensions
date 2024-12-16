package de.intension.authentication.authenticators.persistence.jpa;

import de.intension.authentication.authenticators.persistence.LicenseLookupProvider;
import de.intension.authentication.authenticators.persistence.LicenseLookupProviderFactory;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class JpaLicenseLookupProviderFactory implements LicenseLookupProviderFactory {
    public static final String PROVIDER_ID = "jpa-licenceLookup";

    @Override
    public LicenseLookupProvider create(KeycloakSession keycloakSession) {
        return new JpaLicenseLookupProvider(keycloakSession);
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
