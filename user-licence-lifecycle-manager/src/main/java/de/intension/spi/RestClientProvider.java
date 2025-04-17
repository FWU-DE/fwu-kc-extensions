package de.intension.spi;

import de.intension.rest.licence.client.LicenceConnectRestClient;
import org.keycloak.provider.Provider;

public interface RestClientProvider
        extends Provider {

    LicenceConnectRestClient getLicenseConnectRestClient();
}
