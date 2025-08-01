package de.intension.school.resources;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.List;

public class SchoolResourceProvider
        implements AdminRealmResourceProvider {

    private final List<String> validDomains;
    private final String principalRole;
    private final String teacherRole;

    public SchoolResourceProvider(List<String> validDomains, String principalRole, String teacherRole) {
        this.validDomains = validDomains;
        this.principalRole = principalRole;
        this.teacherRole = teacherRole;
    }

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        return new SchoolResource(session, realm, auth, validDomains, principalRole, teacherRole);
    }

    @Override
    public void close() {
        // nothing to close
    }
}
