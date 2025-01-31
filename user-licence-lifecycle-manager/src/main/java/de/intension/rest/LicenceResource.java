package de.intension.rest;

import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.keycloak.models.KeycloakSession;

/**
 * Custom rest endpoints exposed via <code>${authUrl}/realms/${realm}/licences-resource/</code>.
 */
@RequiredArgsConstructor
public class LicenceResource {

    private final KeycloakSession session;

    /**
     * Get licences via their ID, which is the user's ID hashed with HMAC.
     *
     * <pre>
     *     GET ${authUrl}/realms/${realm}/licences-resource/${hmacId}
     * </pre>
     */
    @Path("/{hmac-id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLicence(@PathParam("hmac-id") String hmacID) {
        var licence = session.getProvider(LicenceJpaProvider.class).getLicenceByHmacId(hmacID);
        if (licence == null) {
            throw new NotFoundException("No licence with hmac-id " + hmacID + " found.");
        }
        return licence;
    }
}
