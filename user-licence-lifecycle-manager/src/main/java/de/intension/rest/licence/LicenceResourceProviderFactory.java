package de.intension.rest.licence;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import de.intension.config.ConfigConstant;
import lombok.RequiredArgsConstructor;

/**
 * Provider factory for {@link LicenceResourceProvider}.
 */
@RequiredArgsConstructor
public class LicenceResourceProviderFactory implements RealmResourceProviderFactory {

    private String schoolIdsAttribute;
    
    @Override
    public RealmResourceProvider create(KeycloakSession keycloakSession) {
        return new LicenceResourceProvider(keycloakSession, schoolIdsAttribute);
    }

    @Override
    public void init(Config.Scope scope) {
            schoolIdsAttribute = scope.get(ConfigConstant.SCHOOL_IDS_ATTRIBUTE.asString());
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "licences";
    }
}
