package de.intension.rest.licence;

import static de.intension.rest.licence.model.LicenseConstants.BUNDESLAND_ATTRIBUTE;
import static de.intension.rest.licence.model.LicenseConstants.CLIENT_NAME;
import static de.intension.rest.licence.model.LicenseConstants.SCHULNUMMER;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.services.resource.RealmResourceProvider;

import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import de.intension.rest.licence.client.LicenceConnectRestClient;
import de.intension.spi.RestClientProvider;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Custom rest endpoints exposed via <code>${authUrl}/realms/${realm}/licences/</code>.
 */
public class LicenceResourceProvider implements RealmResourceProvider {

    private static final Logger logger = Logger.getLogger(LicenceResourceProvider.class);

    private String                schoolIdsAttribute;

    private final KeycloakSession session;

    public LicenceResourceProvider(KeycloakSession session, String schoolIdsAttribute)
    {
        this.session = session;
        this.schoolIdsAttribute = schoolIdsAttribute;
    }

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

    /**
     * Get licences for the currently authenticated user by calling the generic licence connect
     * service directly. User attributes are used to populate the required query parameters.
     *
     * <pre>
     *     GET ${authUrl}/realms/${realm}/licences
     * </pre>
     *
     * @param clientName optional client name override; defaults to the {@code issuedFor} claim of the bearer token
     * @param schoolIdsAttribute optional name of the user attribute that holds school ids; defaults to {@code prefixedSchools}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLicencesForCurrentUser(
        @QueryParam("clientName") String clientName)
    {
        var auth = checkAuth();
        UserModel user = auth.getUser();
        AccessToken token = auth.getToken();

        String resolvedClientName = (clientName != null && !clientName.isBlank())
                ? clientName
                : token.getIssuedFor();

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put(BUNDESLAND_ATTRIBUTE, user.getFirstAttribute(BUNDESLAND_ATTRIBUTE));
        queryParams.put(SCHULNUMMER, user.getFirstAttribute(schoolIdsAttribute));
        queryParams.put(CLIENT_NAME, resolvedClientName);

        LicenceConnectRestClient restClient = session.getProvider(RestClientProvider.class).getLicenseConnectRestClient();
        try {
            String licences = restClient.getLicences(queryParams);
            return (licences != null) ? licences : "{}";
        } catch (IllegalArgumentException ex) {
            logger.errorf("User missing parameters for licence connect call: %s", ex.getMessage());
            throw new BadRequestException(ex.getMessage());
        } catch (IOException ex) {
            logger.errorf("Error while fetching licences for user %s: %s", user.getUsername(), ex.getMessage());
            throw new InternalServerErrorException("Error fetching licences from licence connect service");
        }
    }

    private AppAuthManager.AuthResult checkAuth() {
        var auth = new BearerTokenAuthenticator(session).authenticate();
        if (auth == null) {
            throw new NotAuthorizedException("Bearer");
        }
        return auth;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {

    }
}
