package de.intension.spi;

import org.keycloak.provider.Provider;

import de.intension.rest.LicenseConnectRestClient;

public interface RestClientProvider
    extends Provider
{

    LicenseConnectRestClient restClient();

}
