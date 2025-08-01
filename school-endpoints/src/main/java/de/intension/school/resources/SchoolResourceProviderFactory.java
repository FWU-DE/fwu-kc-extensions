package de.intension.school.resources;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

import java.util.List;

public class SchoolResourceProviderFactory
        implements AdminRealmResourceProviderFactory {

    public static final String PROVIDER_ID = "schools";

    private boolean enabled;
    private List<String> validDomains;
    private String principalRole;
    private String teacherRole;

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        if (enabled) {
            return new SchoolResourceProvider(validDomains, principalRole, teacherRole);
        }
        return null;
    }

    @Override
    public void init(Config.Scope config) {
        enabled = Boolean.parseBoolean(config.get("enabled"));
        validDomains = List.of(config.getArray("validDomains"));
        principalRole = config.get("principalRole");
        teacherRole = config.get("teacherRole");
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // nothing to do
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
