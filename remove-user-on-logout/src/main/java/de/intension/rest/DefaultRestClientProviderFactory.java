package de.intension.rest;

import java.io.IOException;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import de.intension.config.ConfigConstant;
import de.intension.spi.RestClientProvider;
import de.intension.spi.RestClientProviderFactory;

public class DefaultRestClientProviderFactory
    implements RestClientProviderFactory
{

    private static final String                       PROVIDER_ID       = "default";
    private static final Logger                       LOG               = Logger.getLogger(DefaultRestClientProviderFactory.class);

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
        .property()
        .name(ConfigConstant.LICENSE_CONNECT_BASE_URL.asString())
        .type(ProviderConfigProperty.STRING_TYPE)
        .label("Base URL")
        .helpText("Base URL of the license connect API")
        .add()
        .property()
        .name(ConfigConstant.LICENSE_CONNECT_API_KEY.asString())
        .type(ProviderConfigProperty.STRING_TYPE)
        .label("API key")
        .helpText("Key used for authentication to connect with API")
        .add()
        .build();

    private final ThreadLocal<Boolean>                initHolder        = new ThreadLocal<>();
    private LicenseConnectRestClient                  restClient;
    public String                                     licenseConnectBaseUrl;
    public String                                     licenseConnectAPIKey;

    @Override
    public RestClientProvider create(KeycloakSession session)
    {
        if (initHolder.get() == null) {
            synchronized(this) {
                if (restClient == null) {
                    restClient = new LicenseConnectRestClient(this.licenseConnectBaseUrl, this.licenseConnectAPIKey);
                }
                initHolder.set(Boolean.TRUE);
            }
        }
        return new DefaultRestClientProvider(restClient);
    }

    @Override
    public void init(Scope config)
    {
        this.licenseConnectBaseUrl = config.get(ConfigConstant.LICENSE_CONNECT_BASE_URL.asString());
        this.licenseConnectAPIKey = config.get(ConfigConstant.LICENSE_CONNECT_API_KEY.asString());
        if (this.licenseConnectBaseUrl == null || this.licenseConnectAPIKey == null) {
            LOG.warn("License connect URL and the API key should be added in order to access license connect API");
        }

    }

    @Override
    public void postInit(KeycloakSessionFactory factory)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata()
    {
        return CONFIG_PROPERTIES;
    }

    @Override
    public void close()
    {
        initHolder.set(Boolean.FALSE);
        if (this.restClient != null) {
            try {
                this.restClient.close();
            } catch (IOException e) {
                LOG.warn("There was error while closing the rest client");
            }
        }
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

}
