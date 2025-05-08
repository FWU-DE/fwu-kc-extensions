package de.intension.rest.licence;

import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

/**
 * Custom rest endpoints exposed via <code>${authUrl}/realms/${realm}/licences/</code>.
 */
@RequiredArgsConstructor
public class LicenceResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    /**
     * Get licences via their ID, which is the user's ID hashed with HMAC.
     *
     * <pre>
     *     GET ${authUrl}/realms/${realm}/licences/${hmacId}
     * </pre>
     */
    @Path("/{hmac-id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLicence(@PathParam("hmac-id") String hmacID) {
        checkAuth();
        var license = session.getProvider(LicenceJpaProvider.class).getLicenceByHmacId(hmacID);
        return (license != null) ? license : "{}";
    }

    private void checkAuth() {
        var auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        if (auth == null) {
            throw new NotAuthorizedException("Bearer");
        }
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }
}
