package de.intension.rest.licence.client;

import de.intension.spi.RestClientProvider;

public class DefaultRestClientProvider
        implements RestClientProvider {

    private final LicenceConnectRestClient    licenceConnectRestClient;

    public DefaultRestClientProvider(LicenceConnectRestClient licenceConnectRestClient) {
        this.licenceConnectRestClient = licenceConnectRestClient;
    }

    @Override
    public void close()
    {
        // Nothing to do
    }

    @Override
    public LicenceConnectRestClient getLicenseConnectRestClient()
    {
        return this.licenceConnectRestClient;
    }

}
