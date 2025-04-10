package de.intension.spi;

import de.intension.rest.licence.client.LegacyLicenceConnectRestClient;
import org.keycloak.provider.Provider;

public interface RestClientProvider
        extends Provider {

    LegacyLicenceConnectRestClient restClient();

}
