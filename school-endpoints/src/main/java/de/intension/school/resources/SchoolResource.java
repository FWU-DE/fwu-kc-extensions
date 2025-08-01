package de.intension.school.resources;

import de.intension.school.dto.SchoolPrincipal;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.*;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.List;

public class SchoolResource
        implements AdminRealmResourceProvider {

    private final KeycloakSession session;
    private RealmModel realm;
    private AdminPermissionEvaluator auth;
    private final List<String> validDomains;
    private final String principalRole;
    private final String teacherRole;

    public SchoolResource(KeycloakSession session, List<String> validDomains, String principalRole, String teacherRole) {
        this.session = session;
        this.validDomains = validDomains;
        this.principalRole = principalRole;
        this.teacherRole = teacherRole;
    }

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        return this;
    }

    @Override
    public void close() {
        // nothing to close
    }

    @POST
    @Path("/principal")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSchoolPrincipal(SchoolPrincipal principal) {
        RoleModel role = session.roles().getRealmRole(realm, principalRole);
        if (role == null) {
            throw new InternalServerErrorException("School principal role not configured in realm");
        }
        String email = principal.email();
        if (email == null || email.contains("@")) {
            throw new BadRequestException("Invalid email address");
        }
        String domain = email.split("@")[1];
        if (!validDomains.contains(domain)) {
            throw new BadRequestException("Invalid school domain");
        }
        Integer schoolId = parseSchoolId(email.split("@")[0]);
        UserModel user = session.users().addUser(realm, email);
        GroupModel group = session.groups().getGroupByName(realm, null, schoolId.toString());
        if (group == null) {
            session.groups().createGroup(realm, schoolId.toString());
        }
        user.joinGroup(group);
        user.grantRole(role);
        return Response.status(Response.Status.CREATED).entity(user).build();
    }

    private Integer parseSchoolId(String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid school id in email");
        }
    }
}
