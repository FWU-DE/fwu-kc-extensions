package de.intension.authentication.authenticators.backup.jpa.entity;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Arrays;
import java.util.List;

public class LicenceEntityProvider implements JpaEntityProvider {
    private static Class<?>[] entities = {LicenceEntity.class};

    @Override
    public List<Class<?>> getEntities() {
        return Arrays.<Class<?>>asList(entities);
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