package de.intension.authentication.authenticators.jpa;

import org.keycloak.provider.Spi;

/**
 * Custom Spi so that the {@link LicenceJpaProvider} can be found
 * as a provider for persisting licences, finding persisted licences and deleting licences from the keycloak database.
 */
public class LicenceJpaSpi implements Spi {
    private static final String LICENCE_JPA = "licenceJpa";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return LICENCE_JPA;
    }

    @Override
    public Class<LicenceJpaProvider> getProviderClass() {
        return LicenceJpaProvider.class;
    }

    @Override
    public Class<LicenceJpaProviderFactory> getProviderFactoryClass() {
        return LicenceJpaProviderFactory.class;
    }
}
