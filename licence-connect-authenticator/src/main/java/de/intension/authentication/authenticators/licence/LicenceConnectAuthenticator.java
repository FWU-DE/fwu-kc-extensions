package de.intension.authentication.authenticators.licence;

import com.fasterxml.jackson.databind.JsonNode;
import de.intension.authentication.authenticators.rest.LicenceConnectRestClient;
import de.intension.authentication.authenticators.rest.model.LicenceRequest;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.services.ErrorPage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LicenceConnectAuthenticator
    implements Authenticator
{

    private static final Logger      logger                          = Logger.getLogger(LicenceConnectAuthenticator.class);
    public static final String       LICENCE_ATTRIBUTE               = "licences";
    private static final String      SCHOOL_IDENTIFICATION_ATTRIBUTE = "prefixedSchools";
    private static final String      BUNDESLAND_ATTRIBUTE            = "bundesland";
    private static final String      HAS_LICENCES_ATTRIBUTE          = "hasLicences";
    private static final int         PART_SIZE                       = 255;

    private LicenceConnectRestClient restClient;

    public LicenceConnectAuthenticator()
    {
    }

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        this.restClient = createRestClient(context.getAuthenticatorConfig().getConfig());
        if (this.restClient == null) {
            logger.error("Please configure the authenticator");
            context.failure(AuthenticationFlowError.ACCESS_DENIED, createErrorPage(context));
        }
        if (this.addUserLicence(context)) {
            context.success();
        }
        else {
            logger.infof("There were no licences found associated with the user %s", context.getUser().getUsername());
            context.failure(AuthenticationFlowError.ACCESS_DENIED, createErrorPage(context));
        }
    }

    private boolean addUserLicence(AuthenticationFlowContext context)
    {
        UserModel user = context.getUser();
        LicenceRequest licenceRequest = createLicenceRequest(user, context);
        JsonNode userLicences = fetchUserLicence(licenceRequest);
        if (userLicences != null && userLicences.path(HAS_LICENCES_ATTRIBUTE).asBoolean()) {
            String userLicence = userLicences.path(LICENCE_ATTRIBUTE).toString();
            for (int i = 0; i < userLicence.length(); i += PART_SIZE) {
                String partValue = userLicence.substring(i, Math.min(userLicence.length(), i + PART_SIZE));
                user.setAttribute(LICENCE_ATTRIBUTE + (i / PART_SIZE + 1), List.of(partValue));
            }
            return true;
        }
        return false;
    }

    private LicenceConnectRestClient createRestClient(Map<String, String> config)
    {
        if (config.get(LicenceConnectAuthenticatorFactory.LICENCE_URL) == null || config.get(LicenceConnectAuthenticatorFactory.LICENCE_API_KEY) == null) {
            return null;
        }
        LicenceConnectRestClient restClient = new LicenceConnectRestClient(config.get(LicenceConnectAuthenticatorFactory.LICENCE_URL),
                config.get(LicenceConnectAuthenticatorFactory.LICENCE_API_KEY));
        return restClient;
    }

    private LicenceRequest createLicenceRequest(UserModel user, AuthenticationFlowContext context)
    {
        LicenceRequest licenceRequestedRequest = null;
        String schulKennung = user.getFirstAttribute(SCHOOL_IDENTIFICATION_ATTRIBUTE);
        String bundesLand = user.getFirstAttribute(BUNDESLAND_ATTRIBUTE);
        String clientId = context.getAuthenticationSession().getClient().getClientId();

        Optional<FederatedIdentityModel> idp = fetchFederatedIdentityModels(user, context).findFirst();
        if (idp.isPresent()) {
            String userId = idp.get().getUserId();
            licenceRequestedRequest = new LicenceRequest(userId, clientId, schulKennung, bundesLand);
        }

        return licenceRequestedRequest;
    }

    private JsonNode fetchUserLicence(LicenceRequest licenceRequest)
    {
        JsonNode userLicence = null;
        try {
            userLicence = this.restClient.getLicences(licenceRequest);
        } catch (IOException e) {
            logger.errorf(e, "Error while fetching the user licence from licence connect");
        }
        return userLicence;
    }

    private Stream<FederatedIdentityModel> fetchFederatedIdentityModels(UserModel user, AuthenticationFlowContext context)
    {
        RealmModel realm = context.getRealm();
        Set<String> idps = realm.getIdentityProvidersStream().map(IdentityProviderModel::getAlias).collect(Collectors.toSet());
        Stream<FederatedIdentityModel> federatedIdentityModelList = context.getSession().users().getFederatedIdentitiesStream(realm, user)
            .filter(identity -> idps.contains(identity.getIdentityProvider()));
        return federatedIdentityModelList;
    }

    protected Response createErrorPage(AuthenticationFlowContext context)
    {
        return ErrorPage.error(context.getSession(), context.getAuthenticationSession(),
                               Response.Status.FORBIDDEN, "There is no licence associated with user");
    }

    @Override
    public void action(AuthenticationFlowContext context)
    {
        // Nothing to implement
    }

    @Override
    public boolean requiresUser()
    {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user)
    {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user)
    {
        // Nothing to implement
    }

    @Override
    public void close()
    {
        // Nothing to implement
    }

    public LicenceConnectRestClient getRestClient()
    {
        return this.restClient;
    }
}
