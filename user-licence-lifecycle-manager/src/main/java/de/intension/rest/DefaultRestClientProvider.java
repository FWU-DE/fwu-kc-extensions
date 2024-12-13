package de.intension.rest;

import de.intension.spi.RestClientProvider;

public class DefaultRestClientProvider
    implements RestClientProvider
{

    private final LicenceConnectRestClient restClient;

    public DefaultRestClientProvider(LicenceConnectRestClient restClient)
    {
        this.restClient = restClient;
    }

    @Override
    public void close()
    {
        // TODO Auto-generated method stub

    }

    @Override
    public LicenceConnectRestClient restClient()
    {
        return this.restClient;
    }

}
