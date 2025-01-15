package de.intension.authentication.authenticators.jpa.entity;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Provider so that the {@link LicenceEntity} is recognized in keycloaks entity manager.
 */
public class LicenceEntityProvider implements JpaEntityProvider {
    private static final Class<?>[] entities = {LicenceEntity.class};

    @Override
    public List<Class<?>> getEntities() {
        return Arrays.asList(entities);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/licence-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return LicenceEntityProviderFactory.ID;
    }

    @Override
    public void close() {
        //Nothing to do
    }
}
