package de.intension.protocol.oidc.resources;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class HmacMappingResource implements RealmResourceProvider {

    private KeycloakSession session;

    public HmacMappingResource(KeycloakSession session) {

        this.session = session;
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUserId(final HmacMappingRequest request) {
        var client = session.getContext().getRealm().getClientByClientId(request.getClientId());
        if (client == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Client '" + request.getClientId() + "' does not exist")
                    .build();
        }
        var mappers = client.getProtocolMappersStream().filter(m -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(m.getProtocolMapper())).toList();
        if (mappers.size() != 1) {
            String error = mappers.isEmpty() ?
                    "Client does not have protocol mapper '" + HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID + "' configured"
                    : "Client has more than one protocol mapper '" + HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID + "' configured";
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }
        var hmacMapper = mappers.get(0);
        String testValue = request.getTestValue();
        for (String value : request.getOriginalValues()) {
            var encryptedId = HmacPairwiseSubMapperHelper.generateIdentifier(hmacMapper, value);
            if (encryptedId.equals(testValue)) {
                return Response.ok(value).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Override
    public Object getResource() {
        return new HmacMappingResource(session);
    }

    @Override
    public void close() {
    }
}
