package de.intension.authentication.authenticators.license.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.models.KeycloakSession;

import java.util.List;
import java.util.Map;

public class LicenceResource {

    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final KeycloakSession session;
    private final String realmName;

    public LicenceResource(KeycloakSession session, String realmName) {
        this.session = session;
        this.realmName = realmName;
    }

    @Path("/{user-id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLicence(@PathParam("user-id") String userId) {
        // TODO: How to get user
        var user = session.users().getUserById(session.realms().getRealmByName(realmName), userId);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        var licenceBuilder = new StringBuilder();
        var filteredLicenceAttributes = user.getAttributes().entrySet().stream().filter(it -> it.getKey().contains("licences")).toList();
        for (int part = 1; part <= filteredLicenceAttributes.size(); part++) {
            licenceBuilder.append(getLicenceText(filteredLicenceAttributes, part));
        }
        try {
            return mapper.writeValueAsString(mapper.readTree(licenceBuilder.toString()));
        } catch (JsonProcessingException e) {
            throw new InternalServerErrorException("Licence is not valid JSON.");
        }
    }

    private String getLicenceText(List<Map.Entry<String, List<String>>> licenceAttributes, int index) {
        return licenceAttributes.stream()
                .filter(entry -> entry.getKey().equals("licences" + index))
                .findFirst()
                .map(entry -> entry.getValue().stream().findFirst().orElseThrow(() -> new InternalServerErrorException("Licence part " + index + " is empty.")))
                .orElseThrow(() -> new InternalServerErrorException("Licence part " + index + " is missing."));
    }
}
