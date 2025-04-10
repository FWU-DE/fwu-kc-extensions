package de.intension.rest.licence.client;

import de.intension.spi.RestClientProvider;

public class DefaultRestClientProvider
        implements RestClientProvider {

    private final LegacyLicenceConnectRestClient restClient;

    public DefaultRestClientProvider(LegacyLicenceConnectRestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public LegacyLicenceConnectRestClient restClient() {
        return this.restClient;
    }

}
