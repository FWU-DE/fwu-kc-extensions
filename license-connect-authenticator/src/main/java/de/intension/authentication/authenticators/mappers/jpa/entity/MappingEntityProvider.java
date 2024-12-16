package de.intension.authentication.authenticators.mappers.jpa.entity;

import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;

import java.util.Arrays;
import java.util.List;

public class MappingEntityProvider implements JpaEntityProvider {
    private static Class<?>[] entities = {LicenseEntity.class};

    @Override
    public List<Class<?>> getEntities() {
        return Arrays.<Class<?>>asList(entities);
    }

    @Override
    public String getChangelogLocation() {
        return "META-INF/pseudonym-license-mapping-changelog.xml";
    }

    @Override
    public String getFactoryId() {
        return MappingEntityProviderFactory.ID;
    }

    @Override
    public void close() {
        //Nothing to do
    }
}
