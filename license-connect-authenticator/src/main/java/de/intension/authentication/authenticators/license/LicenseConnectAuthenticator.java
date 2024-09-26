package de.intension.authentication.authenticators.license;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPage;

import com.fasterxml.jackson.databind.JsonNode;

import de.intension.authentication.authenticators.rest.LicenseConnectRestClient;
import de.intension.authentication.authenticators.rest.model.LicenseRequestedRequest;
import jakarta.ws.rs.core.Response;

public class LicenseConnectAuthenticator
    implements Authenticator
{

    private static final Logger            logger                          = Logger.getLogger(LicenseConnectAuthenticator.class);
    public static final String             LICENSE_ATTRIBUTE               = "licences";
    private static final String            SCHOOL_IDENTIFICATION_ATTRIBUTE = "prefixedSchools";
    private static final String            BUNDESLAND_ATTRIBUTE            = "bundesland";
    private static final String            HAS_LICENSES_ATTRIBUTE          = "hasLicences";

    private final LicenseConnectRestClient restClient;

    public LicenseConnectAuthenticator(LicenseConnectRestClient restClient)
    {
        this.restClient = restClient;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        if (this.addUserLicense(context)) {
            context.success();
        }
        else {
            logger.infof("There were no licenses found associated with the user %s", context.getUser().getUsername());
            context.failure(AuthenticationFlowError.ACCESS_DENIED, createErrorPage(context));
        }
    }

    private boolean addUserLicense(AuthenticationFlowContext context)
    {
        UserModel user = context.getUser();
        LicenseRequestedRequest licenseRequest = createLicenseRequest(user, context);
        JsonNode userLicenses = fetchUserLicense(licenseRequest);
        if (userLicenses != null && userLicenses.path(HAS_LICENSES_ATTRIBUTE).asBoolean()) {
            int partSize = 255;
            String userLicense = userLicenses.path(LICENSE_ATTRIBUTE).toString();
            for (int i = 0; i < userLicense.length(); i += partSize) {
                String partValue = userLicense.substring(i, Math.min(userLicense.length(), i + partSize));
                user.setAttribute(LICENSE_ATTRIBUTE + (i / partSize + 1), List.of(partValue));
            }
            return true;
        }
        return false;
    }

    private LicenseRequestedRequest createLicenseRequest(UserModel user, AuthenticationFlowContext context)
    {
        LicenseRequestedRequest licenseRequestedRequest = null;
        String schulKennung = user.getFirstAttribute(SCHOOL_IDENTIFICATION_ATTRIBUTE);
        String bundesLand = user.getFirstAttribute(BUNDESLAND_ATTRIBUTE);
        String clientId = context.getAuthenticationSession().getClient().getClientId();

        Optional<FederatedIdentityModel> idp = fetchFederatedIdentityModels(user, context).findFirst();
        if (idp.isPresent()) {
            String userId = idp.get().getUserId();
            licenseRequestedRequest = new LicenseRequestedRequest(userId, clientId, schulKennung, bundesLand);
        }

        return licenseRequestedRequest;
    }

    private JsonNode fetchUserLicense(LicenseRequestedRequest licenseRequest)
    {
        JsonNode userLicense = null;
        try {
            userLicense = this.restClient.getLicenses(licenseRequest);
        } catch (IOException e) {
            logger.errorf(e, "Error while fetching the user license from license connect");
        }
        return userLicense;
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
                               Response.Status.FORBIDDEN, "There is no license associated with user");
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

    public LicenseConnectRestClient getRestClient()
    {
        return this.restClient;
    }
}
