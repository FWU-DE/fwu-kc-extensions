package de.intension.authentication.authenticators.licence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import de.intension.authentication.authenticators.jpa.entity.LicenceEntity;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import de.intension.rest.licence.client.LicenceConnectRestClient;
import de.intension.spi.RestClientProvider;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.services.ErrorPage;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.intension.authentication.authenticators.licence.LicenceConnectAuthenticatorFactory.GENERIC_LICENSE_CLIENTS;
import static de.intension.authentication.authenticators.licence.LicenceConnectAuthenticatorFactory.BILO_LICENSE_CLIENTS;
import static de.intension.rest.licence.model.LicenseConstants.*;

@Getter
@NoArgsConstructor
public class LicenceConnectAuthenticator
        implements Authenticator {

    private static final Logger       logger       = Logger.getLogger(LicenceConnectAuthenticator.class);
    private final        ObjectMapper objectMapper = new ObjectMapper();
    private static final int          PART_SIZE    = 255;
    private static final String CLIENT_DELIMITER = ",";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getAuthenticatorConfig() == null) {
            logger.errorf("Please configure the license connect authenticator with the clients");
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, createErrorPage(context));
        } else {
            this.addLicenses(context);
            context.success();
        }
    }

    private void addLicenses(AuthenticationFlowContext context) {
        var client = context.getAuthenticationSession().getClient().getClientId();
        Optional<String> licenseType = checkLicenseType(context, client);
        if (licenseType.isPresent()) {
            LicenceConnectRestClient restClient = context.getSession().getProvider(RestClientProvider.class).getLicenseConnectRestClient();
            UserModel user = context.getUser();
            Map<String,String> queryParams = new HashMap<>();
            populateCommonQueryParams(queryParams, context, user);
            this.fetchUserLicenses(licenseType.get(), queryParams, user, client, context, restClient);
        }
    }

    private void fetchUserLicenses(String licenseType, Map<String,String> queryParams, UserModel user, String client, AuthenticationFlowContext context, LicenceConnectRestClient restClient) {
        String userLicences = null;
        try {
            if (BILO_LICENSE_CLIENTS.equals(licenseType)) {
                populateUcsQueryParams(queryParams, user, client);
                userLicences = restClient.getUcsLicences(queryParams);
            } else if (GENERIC_LICENSE_CLIENTS.equals(licenseType)) {
                populateLicenseConnectQueryParams(queryParams, user, client);
                userLicences = restClient.getLicences(queryParams);
            }
        } catch (WebApplicationException | IOException ex) {
            logger.errorf("Error while fetching the user license for the user with username %s. Response from server %s", user.getUsername(), ex.getMessage());
            context.failure(AuthenticationFlowError.ACCESS_DENIED, createErrorPage(context));
        }
        if (!StringUtil.isBlank(userLicences)) {
            logger.infof("User license found for the user %s from the license type %s", user.getUsername(), licenseType);
            for (int i = 0; i < userLicences.length(); i += PART_SIZE) {
                String partValue = userLicences.substring(i, Math.min(userLicences.length(), i + PART_SIZE));
                user.setAttribute(LICENCE_ATTRIBUTE + (i / PART_SIZE + 1), List.of(partValue));
            }
            this.persistUserLicense(context, user, userLicences);
        }

    }

    private Optional<String> checkLicenseType(AuthenticationFlowContext context, String clientName) {
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        return config.entrySet().stream()
                .filter(entry -> Arrays.stream(entry.getValue().split(CLIENT_DELIMITER))
                        .map(String::trim)
                        .anyMatch(client -> client.equals(clientName)))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private void populateCommonQueryParams(Map<String,String> queryParams, AuthenticationFlowContext context, UserModel user) {
        queryParams.put(BUNDESLAND_ATTRIBUTE, user.getFirstAttribute(BUNDESLAND_ATTRIBUTE));
        Optional<FederatedIdentityModel> idp = fetchFederatedIdentityModels(user, context).findFirst();
        if (idp.isPresent()) {
            String userId = idp.get().getUserId();
            queryParams.put(USER_ID, userId);
        }
    }

    private void populateUcsQueryParams(Map<String,String> queryParams, UserModel user, String clientId) {
        queryParams.put(SCHULKENNUNG, user.getFirstAttribute(SCHOOL_IDENTIFICATION_ATTRIBUTE));
        queryParams.put(CLIENT_ID, clientId);
    }

    private void populateLicenseConnectQueryParams(Map<String,String> queryParams, UserModel user, String clientId) {
        // TODO: Add the params for the standortnummer
        queryParams.put(SCHULNUMMER, user.getFirstAttribute(SCHOOL_IDENTIFICATION_ATTRIBUTE));
        queryParams.put(CLIENT_NAME, clientId);
    }

    private void persistUserLicense(AuthenticationFlowContext context, UserModel user, String userLicence) {
        var hmacMapper = context.getAuthenticationSession().getClient().getProtocolMappersStream()
                .filter(mapper -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(mapper.getProtocolMapper())).findFirst();
        if (hmacMapper.isPresent()) {
            String hmacId = HmacPairwiseSubMapperHelper.generateIdentifier(hmacMapper.get(), user);
            LicenceEntity licence = new LicenceEntity(hmacId, userLicence);
            context.getSession().getProvider(LicenceJpaProvider.class).persistLicence(licence);
            logger.infof("User licence has been persisted in the database for user %s", user.getUsername());
        }
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
