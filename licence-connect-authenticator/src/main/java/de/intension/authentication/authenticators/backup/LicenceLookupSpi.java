package de.intension.authentication.authenticators.backup;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class LicenceLookupSpi implements Spi {
    private static final String LICENCE_LOOKUP = "licenceLookup";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return LICENCE_LOOKUP;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return LicenceLookupProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return LicenceLookupProviderFactory.class;
    }
}
