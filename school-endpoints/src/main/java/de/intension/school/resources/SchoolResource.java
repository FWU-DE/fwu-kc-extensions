package de.intension.school.resources;

import de.intension.school.dto.SchoolPrincipal;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.*;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import java.util.List;

public class SchoolResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final List<String> validDomains;
    private final String principalRole;
    private final String teacherRole;

    public SchoolResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, List<String> validDomains, String principalRole, String teacherRole) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.validDomains = validDomains;
        this.principalRole = principalRole;
        this.teacherRole = teacherRole;
    }

    @POST
    @Path("/principal")
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
