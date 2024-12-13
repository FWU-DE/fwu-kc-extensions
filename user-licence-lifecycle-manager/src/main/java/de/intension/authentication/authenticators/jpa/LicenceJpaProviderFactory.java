package de.intension.authentication.authenticators.jpa;

import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderFactory;

public class LicenceJpaProviderFactory implements ProviderFactory<LicenceJpaProvider> {
    public static final String PROVIDER_ID = "licence-jpa";

    @Override
    public LicenceJpaProvider create(KeycloakSession keycloakSession) {
        return new LicenceJpaProvider(keycloakSession);
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
