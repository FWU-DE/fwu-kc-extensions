package de.intension.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class RestClientSpi
        implements Spi {

    private static final String SPI_NAME = "restClient";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return RestClientProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory<? extends Provider>> getProviderFactoryClass() {
        return RestClientProviderFactory.class;
    }

}
