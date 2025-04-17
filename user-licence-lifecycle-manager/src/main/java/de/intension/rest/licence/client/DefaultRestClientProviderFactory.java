package de.intension.rest.licence.client;

import de.intension.config.ConfigConstant;
import de.intension.spi.RestClientProvider;
import de.intension.spi.RestClientProviderFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.utils.StringUtil;

public class DefaultRestClientProviderFactory
        implements RestClientProviderFactory {

    private static final String PROVIDER_ID = "default";
    private static final Logger LOG = Logger.getLogger(DefaultRestClientProviderFactory.class);
    private final ThreadLocal<Boolean>        initHolder = new ThreadLocal<>();
    private       LicenceConnectRestClient    licenceConnectRestClient;

    public String licenceConnectBaseUrl;
    public String licenceConnectAPIKey;

    @Override
    public RestClientProvider create(KeycloakSession session) {
        if (initHolder.get() == null) {
            synchronized (this) {
                if (licenceConnectRestClient == null) {
                    licenceConnectRestClient = new LicenceConnectRestClient(session, this.licenceConnectBaseUrl, this.licenceConnectAPIKey);
                }
                initHolder.set(Boolean.TRUE);
            }
        }
        return new DefaultRestClientProvider(licenceConnectRestClient);
    }

    @Override
    public void init(Scope config) {

        this.licenceConnectBaseUrl = config.get(ConfigConstant.LICENCE_CONNECT_BASE_URL.asString());
        this.licenceConnectAPIKey = config.get(ConfigConstant.LICENCE_CONNECT_API_KEY.asString());

        if (StringUtil.isBlank(licenceConnectBaseUrl) || StringUtil.isBlank(licenceConnectAPIKey)) {
            LOG.warn("Licence connect URL and the API key should be added in order to access licence connect API");
        }

    }

    @Override
    public void postInit(KeycloakSessionFactory factory)
    {
        // Nothing to do
    }

    @Override
    public void close()
    {
        // Nothing to do
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

}
