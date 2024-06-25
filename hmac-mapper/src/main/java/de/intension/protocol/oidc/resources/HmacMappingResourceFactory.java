package de.intension.protocol.oidc.resources;

import org.keycloak.Config;
import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

public class HmacMappingResourceFactory
        implements AdminRealmResourceProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "hmac";
    public static final String VERIFIER_REALM = "verifier-realm";
    public static final String MANAGEMENT_REALM = "management-realm";
    private String verifierRealm;
    private String managementRealm;

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        return new HmacMappingResource(session, this.verifierRealm, this.managementRealm);
    }

    @Override
    public void init(Scope config) {
        this.verifierRealm = config.get(VERIFIER_REALM);
        this.managementRealm = config.get(MANAGEMENT_REALM);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    /**
     * {@link EnvironmentDependentProviderFactory#isSupported()} has been deprecated!
     */
    @Override
    public boolean isSupported() {
        return this.isSupported(null);
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.ADMIN_API);
    }
}
