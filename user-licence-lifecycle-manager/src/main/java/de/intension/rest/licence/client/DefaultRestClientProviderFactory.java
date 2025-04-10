package de.intension.rest.licence.client;

import de.intension.config.ConfigConstant;
import de.intension.spi.RestClientProvider;
import de.intension.spi.RestClientProviderFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import java.io.IOException;
import java.util.List;

public class DefaultRestClientProviderFactory
        implements RestClientProviderFactory {

    private static final String PROVIDER_ID = "default";
    private static final Logger LOG = Logger.getLogger(DefaultRestClientProviderFactory.class);

    private static final List<ProviderConfigProperty> CONFIG_PROPERTIES = ProviderConfigurationBuilder.create()
            .property()
            .name(ConfigConstant.LICENCE_CONNECT_BASE_URL.asString())
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("Base URL")
            .helpText("Base URL of the licence connect API")
            .add()
            .property()
            .name(ConfigConstant.LICENCE_CONNECT_API_KEY.asString())
            .type(ProviderConfigProperty.STRING_TYPE)
            .label("API key")
            .helpText("Key used for authentication to connect with API")
            .add()
            .build();

    private final ThreadLocal<Boolean> initHolder = new ThreadLocal<>();
    private LegacyLicenceConnectRestClient restClient;
    public String licenceConnectBaseUrl;
    public String licenceConnectAPIKey;

    @Override
    public RestClientProvider create(KeycloakSession session) {
        if (initHolder.get() == null) {
            synchronized (this) {
                if (restClient == null) {
                    restClient = new LegacyLicenceConnectRestClient(this.licenceConnectBaseUrl, this.licenceConnectAPIKey);
                }
                initHolder.set(Boolean.TRUE);
            }
        }
        return new DefaultRestClientProvider(restClient);
    }

    @Override
    public void init(Scope config) {
        this.licenceConnectBaseUrl = config.get(ConfigConstant.LICENCE_CONNECT_BASE_URL.asString());
        this.licenceConnectAPIKey = config.get(ConfigConstant.LICENCE_CONNECT_API_KEY.asString());
        if (this.licenceConnectBaseUrl == null || this.licenceConnectAPIKey == null) {
            LOG.warn("Licence connect URL and the API key should be added in order to access licence connect API");
        }

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return CONFIG_PROPERTIES;
    }

    @Override
    public void close() {
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
    public String getId() {
        return PROVIDER_ID;
    }

}
