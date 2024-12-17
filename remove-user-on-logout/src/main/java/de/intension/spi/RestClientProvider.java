package de.intension.spi;

import org.keycloak.provider.Provider;

import de.intension.rest.LicenceConnectRestClient;

public interface RestClientProvider
    extends Provider
{

    LicenceConnectRestClient restClient();

}
