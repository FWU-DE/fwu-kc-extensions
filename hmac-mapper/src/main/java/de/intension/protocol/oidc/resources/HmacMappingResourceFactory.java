package de.intension.protocol.oidc.resources;

import org.keycloak.Config.Scope;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

public class HmacMappingResourceFactory
    implements AdminRealmResourceProviderFactory, EnvironmentDependentProviderFactory
{

    public static final String PROVIDER_ID    = "hmac";
    public static final String VERIFIER_REALM = "verifier-realm";
    private String             verifierRealm;

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session)
    {
        return new HmacMappingResource(session, this.verifierRealm);
    }

    @Override
    public void init(Scope config)
    {
        this.verifierRealm = config.get(VERIFIER_REALM);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory)
    {
    }

    @Override
    public void close()
    {
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported()
    {
        return Profile.isFeatureEnabled(Profile.Feature.ADMIN_API);
    }

}
