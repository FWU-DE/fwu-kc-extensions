package de.intension.authentication.authenticators.licence;

import com.fasterxml.jackson.databind.JsonNode;
import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import de.intension.authentication.authenticators.jpa.entity.LicenceEntity;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import de.intension.rest.licence.client.LegacyLicenceConnectRestClient;
import de.intension.rest.licence.client.LicenceConnectRestClient;
import de.intension.rest.licence.model.LegacyLicenceRequest;
import de.intension.rest.licence.model.LicenceRequest;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.services.ErrorPage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@NoArgsConstructor
public class LicenceConnectAuthenticator
        implements Authenticator {

    private static final Logger logger = Logger.getLogger(LicenceConnectAuthenticator.class);
    public static final String LICENCE_ATTRIBUTE = "licences";
    private static final String SCHOOL_IDENTIFICATION_ATTRIBUTE = "prefixedSchools";
    private static final String BUNDESLAND_ATTRIBUTE = "bundesland";
    private static final int PART_SIZE = 255;

    private LegacyLicenceConnectRestClient legacyRestClient;
    private LicenceConnectRestClient restClient;

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        var realm = context.getRealm().getName();
        if (realm.equals("some_realm")) {
            restClient = createRestClient(context.getAuthenticatorConfig().getConfig());
            if (legacyRestClient == null) {
                logger.error("Legacy authenticator is not configured correctly");
                context.failure(AuthenticationFlowError.ACCESS_DENIED, createErrorPage(context));
                return;
            }
            if (addUserLicenceLegacy(context)) {
                context.success();
            } else {
                logger.infof("There were no licences found associated with the user %s", context.getUser().getUsername());
                context.failure(AuthenticationFlowError.ACCESS_DENIED, createErrorPage(context));
            }
        } else {
            legacyRestClient = createLegacyRestClient(context.getAuthenticatorConfig().getConfig());
            if (legacyRestClient == null) {
                logger.error("Legacy authenticator is not configured correctly");
                context.failure(AuthenticationFlowError.ACCESS_DENIED, createErrorPage(context));
                return;
            }
            if (addUserLicenceLegacy(context)) {
                context.success();
            } else {
                logger.infof("There were no licences found associated with the user %s", context.getUser().getUsername());
                context.failure(AuthenticationFlowError.ACCESS_DENIED, createErrorPage(context));
            }
        }
    }

    private boolean addUserLicence(AuthenticationFlowContext context) throws Exception {
        UserModel user = context.getUser();
        var licenceRequest = createLicenceRequest(user, context);
        var userLicences = restClient.getLicences(licenceRequest);
        var licenceAttributes = user.getAttributes();
        for (var i = 0; i < licenceAttributes.size(); i++) {
            var licence = userLicences.get(i);
            for (var j = 0; j < licence.length(); j += PART_SIZE) {
                var index = (j / PART_SIZE);
                var part = licence.substring(j, Math.min(licence.length(), j + PART_SIZE));
                user.setAttribute(LICENCE_ATTRIBUTE + "_" + i + "_" + index, List.of(part));
            }
            var hmacMapper = context.getAuthenticationSession().getClient().getProtocolMappersStream()
                    .filter(mapper -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(mapper.getProtocolMapper())).findFirst();
            if (hmacMapper.isPresent()) {
                String hmacId = HmacPairwiseSubMapperHelper.generateIdentifier(hmacMapper.get(), user);
                LicenceEntity licenceEntity = new LicenceEntity(hmacId, licence);
                context.getSession().getProvider(LicenceJpaProvider.class).persistLicence(licenceEntity);
                logger.infof("User licence has been persisted in the database for user %s", user.getUsername());
            }
        }
        return true;
    }

    private boolean addUserLicenceLegacy(AuthenticationFlowContext context) {
        UserModel user = context.getUser();
        LegacyLicenceRequest licenceRequest = createLegacyLicenceRequest(user, context);
        JsonNode userLicences = fetchUserLicence(licenceRequest);
        if (userLicences != null) {
            String userLicence = userLicences.path("licences").toString();
            if (userLicence == null || userLicence.isBlank()) {
                // fallback to American licences
                userLicence = userLicences.path("licenses").toString();
            }
            for (int i = 0; i < userLicence.length(); i += PART_SIZE) {
                String partValue = userLicence.substring(i, Math.min(userLicence.length(), i + PART_SIZE));
                user.setAttribute(LICENCE_ATTRIBUTE + (i / PART_SIZE + 1), List.of(partValue));
            }
            var hmacMapper = context.getAuthenticationSession().getClient().getProtocolMappersStream()
                    .filter(mapper -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(mapper.getProtocolMapper())).findFirst();
            if (hmacMapper.isPresent()) {
                String hmacId = HmacPairwiseSubMapperHelper.generateIdentifier(hmacMapper.get(), user);
                LicenceEntity licence = new LicenceEntity(hmacId, userLicence);
                context.getSession().getProvider(LicenceJpaProvider.class).persistLicence(licence);
                logger.infof("User licence has been persisted in the database for user %s", user.getUsername());
            }
            return true;
        }
        return false;
    }

    private LicenceConnectRestClient createRestClient(Map<String, String> config) {
        if (config.get(LicenceConnectAuthenticatorFactory.LICENCE_URL) == null || config.get(LicenceConnectAuthenticatorFactory.LICENCE_API_KEY) == null) {
            return null;
        }
        return new LicenceConnectRestClient(config.get(LicenceConnectAuthenticatorFactory.LICENCE_URL), config.get(LicenceConnectAuthenticatorFactory.LICENCE_API_KEY));
    }

    private LegacyLicenceConnectRestClient createLegacyRestClient(Map<String, String> config) {
        if (config.get(LicenceConnectAuthenticatorFactory.LICENCE_URL) == null || config.get(LicenceConnectAuthenticatorFactory.LICENCE_API_KEY) == null) {
            return null;
        }
        return new LegacyLicenceConnectRestClient(config.get(LicenceConnectAuthenticatorFactory.LICENCE_URL), config.get(LicenceConnectAuthenticatorFactory.LICENCE_API_KEY));
    }

    private LicenceRequest createLicenceRequest(UserModel user, AuthenticationFlowContext context) {
        LicenceRequest licenceRequestedRequest = null;
        String schulkennung = user.getFirstAttribute(SCHOOL_IDENTIFICATION_ATTRIBUTE);
        String bundesland = user.getFirstAttribute(BUNDESLAND_ATTRIBUTE);
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        Optional<FederatedIdentityModel> idp = fetchFederatedIdentityModels(user, context).findFirst();
        if (idp.isPresent()) {
            String userId = idp.get().getUserId();
            licenceRequestedRequest = new LicenceRequest(bundesland, bundesland, schulkennung, userId, clientId);
        }
        return licenceRequestedRequest;
    }

    private LegacyLicenceRequest createLegacyLicenceRequest(UserModel user, AuthenticationFlowContext context) {
        LegacyLicenceRequest licenceRequestedRequest = null;
        String schulKennung = user.getFirstAttribute(SCHOOL_IDENTIFICATION_ATTRIBUTE);
        String bundesLand = user.getFirstAttribute(BUNDESLAND_ATTRIBUTE);
        String clientId = context.getAuthenticationSession().getClient().getClientId();

        Optional<FederatedIdentityModel> idp = fetchFederatedIdentityModels(user, context).findFirst();
        if (idp.isPresent()) {
            String userId = idp.get().getUserId();
            licenceRequestedRequest = new LegacyLicenceRequest(userId, clientId, schulKennung, bundesLand);
        }

        return licenceRequestedRequest;
    }

    private JsonNode fetchUserLicence(LegacyLicenceRequest licenceRequest) {
        JsonNode userLicence = null;
        try {
            userLicence = this.legacyRestClient.getLicences(licenceRequest);
        } catch (IOException e) {
            logger.errorf(e, "Error while fetching the user licence from licence connect");
        }
        return userLicence;
    }

    private Stream<FederatedIdentityModel> fetchFederatedIdentityModels(UserModel user, AuthenticationFlowContext context) {
        RealmModel realm = context.getRealm();
        Set<String> idps = realm.getIdentityProvidersStream().map(IdentityProviderModel::getAlias).collect(Collectors.toSet());
        return context.getSession().users().getFederatedIdentitiesStream(realm, user)
                .filter(identity -> idps.contains(identity.getIdentityProvider()));
    }

    protected Response createErrorPage(AuthenticationFlowContext context) {
        return ErrorPage.error(context.getSession(), context.getAuthenticationSession(),
                Response.Status.FORBIDDEN, "There is no licence associated with user");
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // Nothing to implement
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Nothing to implement
    }

    @Override
    public void close() {
        // Nothing to implement
    }
}
