package de.intension.protocol.oidc.resources;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class HmacMappingResource implements RealmResourceProvider {

    private KeycloakSession session;

    private static final String ROLE_NAME = "hmac-mapping-resource";

    public HmacMappingResource(KeycloakSession session) {

        this.session = session;
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUserId(final HmacMappingRequest request) {
        var user = session.getContext().getAuthenticationSession().getAuthenticatedUser();
        var realm = session.getContext().getRealm();
        var role = realm.getRole(ROLE_NAME);
        var client = realm.getClientByClientId(request.getClientId());
        if (role != null) {
            var roptional = user.getRealmRoleMappingsStream()
                    .filter(r -> ROLE_NAME.equals(r.getName()))
                    .findFirst();
            if (roptional.isEmpty()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
            var roleAssignment = roptional.get();
            var clientIds = roleAssignment.getAttributeStream("clientId").toList();
            if (!clientIds.contains(client.getClientId())) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }
        }
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
