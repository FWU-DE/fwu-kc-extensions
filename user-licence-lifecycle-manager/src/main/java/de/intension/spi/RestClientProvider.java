package de.intension.spi;

import de.intension.rest.LicenceConnectRestClient;
import org.keycloak.provider.Provider;

public interface RestClientProvider
    extends Provider
{

    LicenceConnectRestClient restClient();

}
