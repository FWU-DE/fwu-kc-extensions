package de.intension.resources.admin;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

public class VidisAdminRealmResourceProviderFactory implements AdminRealmResourceProviderFactory, EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "vidis-custom";
    private Config.Scope config;

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        return new VidisAdminRealmResourceProvider(session, config);
    }

    @Override
    public void init(Config.Scope config) {
        this.config = config;
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //nothing to do
    }

    @Override
    public void close() {
        //nothing to do
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return true;
    }
}
