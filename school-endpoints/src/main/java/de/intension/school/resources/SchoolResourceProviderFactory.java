package de.intension.school.resources;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;

import java.util.List;

public class SchoolResourceProviderFactory
        implements AdminRealmResourceProviderFactory {

    public static final String PROVIDER_ID = "schools";

    private static final Logger logger = Logger.getLogger(SchoolResourceProviderFactory.class);

    private List<String> enabledRealms;
    private List<String> validDomains;
    private String principalRole;
    private String teacherRole;

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        String realmName = session.getContext().getRealm().getName();
        if (enabledRealms.contains(realmName)) {
            logger.infof("School endpoints for realm %s are enabled with %s as principal role and %s as teacher role. Valid domains are %s.",
                    realmName, principalRole, teacherRole, validDomains);
            return new SchoolResource(session, validDomains, principalRole, teacherRole);
        }
        logger.infof("School endpoints are disabled for realm %s. To enable, add realm to 'realms' configuration.", realmName);
        return null;
    }

    @Override
    public void init(Config.Scope config) {
        enabledRealms = List.of(config.getArray("realms"));
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
