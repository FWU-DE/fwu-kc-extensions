package de.intension.protocol.oidc.resources;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.resource.RealmResourceProvider;

public class HmacMappingResource implements RealmResourceProvider {

    private KeycloakSession session;

    private static final String ATTRIBUTE_NAME = "hmac-clientId";

    public HmacMappingResource(KeycloakSession session) {
        this.session = session;
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUserId(final HmacMappingRequest request) {
        var client = checkAccess(request.getClientId());

        var hmacMapper = getHmacMapper(client);

        String testValue = request.getTestValue();
        for (String value : request.getOriginalValues()) {
            var encryptedId = HmacPairwiseSubMapperHelper.generateIdentifier(hmacMapper, value);
            if (encryptedId.equals(testValue)) {
                return Response.ok(value).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Check whether requesting user has access to request for this client.
     * User needs to have attribute "hmac-clientId" matching the client ID.
     *
     * @return Client found with parameter clientId
     */
    private ClientModel checkAccess(String clientId) {
        var realm = session.getContext().getRealm();
        var client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new NotFoundException("Client '" + clientId + "' does not exist");
        }
        var user = new AppAuthManager.BearerTokenAuthenticator(session).authenticate().getSession().getUser();
        var clientIds = user.getAttributeStream(ATTRIBUTE_NAME).toList();
        if (!clientIds.contains(client.getClientId())) {
            throw new ForbiddenException();
        }
        return client;
    }

    /**
     * Get the mapper with id "oidc-hmac-pairwise-subject-mapper" from the client's configuration.
     */
    private ProtocolMapperModel getHmacMapper(ClientModel client) {
        var mappers = client.getProtocolMappersStream().filter(m -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(m.getProtocolMapper())).toList();
        if (mappers.size() != 1) {
            String error = mappers.isEmpty() ?
                    "Client does not have protocol mapper '" + HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID + "' configured"
                    : "Client has more than one protocol mapper '" + HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID + "' configured";
            throw new BadRequestException(error);
        }
        return mappers.get(0);
    }

    @Override
    public Object getResource() {
        return new HmacMappingResource(session);
    }

    @Override
    public void close() {
    }
}
