package de.intension.rest;

import de.intension.spi.RestClientProvider;

public class DefaultRestClientProvider
    implements RestClientProvider
{

    private final LicenseConnectRestClient restClient;

    public DefaultRestClientProvider(LicenseConnectRestClient restClient)
    {
        this.restClient = restClient;
    }

    @Override
    public void close()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public LicenseConnectRestClient restClient()
    {
        return this.restClient;
    }

}
