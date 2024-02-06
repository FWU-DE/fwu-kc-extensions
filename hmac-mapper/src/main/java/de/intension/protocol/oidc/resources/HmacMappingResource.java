package de.intension.protocol.oidc.resources;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class HmacMappingResource
    implements AdminRealmResourceProvider
{

    private final KeycloakSession session;

    private String                verifierRealm;
    private String                managementRealm;

    private static final String   ATTRIBUTE_NAME = "hmac-clientId";

    private final Logger          logger         = Logger.getLogger(this.getClass());

    public HmacMappingResource(KeycloakSession session, String verifierRealm, String managementRealm)
    {
        this.session = session;
        this.verifierRealm = verifierRealm;
        this.managementRealm = managementRealm;
    }

    @Override
    public void close()
    {
    }

    @Override
    public Object getResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent)
    {
        return this;
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response getUserId(final HmacMappingRequest request)
    {
        var client = checkAccess(request.getClientId());

        var hmacMapper = getHmacMapper(client);

        String testValue = request.getTestValue();
        for (String value : request.getOriginalValues()) {
            var encryptedId = HmacPairwiseSubMapperHelper.generateIdentifier(hmacMapper, value);
            logger.debugf("Encrypted value for original value '%s' is '%s'", testValue, encryptedId);
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
    private ClientModel checkAccess(String clientId)
    {
        RealmModel managementRealm = this.session.realms().getRealmByName(this.managementRealm);
        var authenticate = new AppAuthManager.BearerTokenAuthenticator(session).setRealm(managementRealm).authenticate();
        if (authenticate == null) {
            logger.warn("Unauthorized request to resource");
            throw new ClientErrorException(Response.Status.UNAUTHORIZED);
        }
        var user = authenticate.getSession().getUser();
        var realm = session.realms().getRealmByName(this.verifierRealm);
        var client = realm.getClientByClientId(clientId);
        if (client == null) {
            logger.warnf("Request from user '%s' for unknown client '%s'", user.getUsername(), clientId);
            throw new NotFoundException("Client '" + clientId + "' does not exist");
        }
        var clientIds = user.getAttributeStream(ATTRIBUTE_NAME).toList();
        if (!clientIds.contains(client.getClientId())) {
            logger.warnf("Request from user '%s' for forbidden client '%s'", user.getUsername(), clientId);
            throw new ForbiddenException();
        }
        return client;
    }

    /**
     * Get the mapper with id "oidc-hmac-pairwise-subject-mapper" from the client's configuration.
     */
    private ProtocolMapperModel getHmacMapper(ClientModel client)
    {
        var mappers = client.getProtocolMappersStream().filter(m -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(m.getProtocolMapper())).toList();
        if (mappers.size() != 1) {
            String error = mappers.isEmpty() ? "Client does not have protocol mapper '" + HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID + "' configured"
                    : "Client has more than one protocol mapper '" + HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID + "' configured";
            logger.warn(error);
            throw new BadRequestException(error);
        }
        return mappers.get(0);
    }

}
