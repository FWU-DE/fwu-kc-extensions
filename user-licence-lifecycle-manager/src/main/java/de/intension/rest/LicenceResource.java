package de.intension.rest;

import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.KeycloakSession;

public class LicenceResource {

    private final KeycloakSession session;

    public LicenceResource(KeycloakSession session) {
        this.session = session;
    }

    @Path("/{hmac-id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLicence(@PathParam("hmac-id") String hmacID) {
        var licence =  session.getProvider(LicenceJpaProvider.class).getLicenceByHmacId(hmacID);
        if(licence == null) {
            throw new NotFoundException("No licence with hmac-id " + hmacID + " found.");
        }
        return licence;
    }
}
