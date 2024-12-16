package de.intension.authentication.authenticators.persistence.jpa.entity;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Arrays;
import java.util.List;

public class LicenseEntityProvider implements JpaEntityProvider {
    private static Class<?>[] entities = {LicenseEntity.class};

    @Override
    public List<Class<?>> getEntities() {
        return Arrays.<Class<?>>asList(entities);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/license-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return LicenseEntityProviderFactory.ID;
    }

    @Override
    public void close() {
        //Nothing to do
    }
}
