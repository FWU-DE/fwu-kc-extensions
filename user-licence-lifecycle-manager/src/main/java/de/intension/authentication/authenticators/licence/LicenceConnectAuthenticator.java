package de.intension.authentication.authenticators.licence;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.intension.authentication.authenticators.jpa.LicenceJpaProvider;
import de.intension.authentication.authenticators.jpa.entity.LicenceEntity;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;
import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapperHelper;
import de.intension.rest.licence.client.LicenceConnectRestClient;
import de.intension.spi.RestClientProvider;
import jakarta.ws.rs.WebApplicationException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.*;
import org.keycloak.utils.StringUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.intension.authentication.authenticators.licence.LicenceConnectAuthenticatorFactory.BILO_LICENSE_CLIENTS;
import static de.intension.authentication.authenticators.licence.LicenceConnectAuthenticatorFactory.GENERIC_LICENSE_CLIENTS;
import static de.intension.rest.licence.model.LicenseConstants.*;

@Getter
@NoArgsConstructor
public class LicenceConnectAuthenticator
        implements Authenticator {

    private static final Logger logger = Logger.getLogger(LicenceConnectAuthenticator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int PART_SIZE = 255;
    private static final String CLIENT_DELIMITER = ",";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getAuthenticatorConfig() == null) {
            logger.errorf("Please configure the license connect authenticator with the clients");
        } else {
            this.addLicenses(context);
        }
        context.success();
    }

    private void addLicenses(AuthenticationFlowContext context) {
        var client = context.getAuthenticationSession().getClient().getClientId();
        Optional<String> licenseType = checkLicenseType(context, client);
        if (licenseType.isPresent()) {
            String licenseTypeValue = licenseType.get();
            LicenceConnectRestClient restClient = context.getSession().getProvider(RestClientProvider.class).getLicenseConnectRestClient();
            UserModel user = context.getUser();
            Map<String, String> queryParams = new HashMap<>();
            populateCommonQueryParams(queryParams, context, user);
            logger.debugf("Fetching user licenses for client '%s' and license type '%s'", client, licenseTypeValue);
            this.fetchUserLicenses(licenseTypeValue, queryParams, user, client, context, restClient);
        }
    }

    private void fetchUserLicenses(String licenseType, Map<String, String> queryParams, UserModel user, String client, AuthenticationFlowContext context, LicenceConnectRestClient restClient) {
        String userLicences = null;
        try {
            String schoolIdsAttribute = getSchoolIdsAttributeName(context);
            String schoolIds = user.getFirstAttribute(schoolIdsAttribute);
            if (BILO_LICENSE_CLIENTS.equals(licenseType)) {
                queryParams.put(SCHULKENNUNG, schoolIds);
                queryParams.put(CLIENT_ID, client);
            } else if (GENERIC_LICENSE_CLIENTS.equals(licenseType)) {
                queryParams.put(SCHULNUMMER, schoolIds);
                queryParams.put(CLIENT_NAME, client);
            }
            userLicences = restClient.getLicences(queryParams);
        } catch (IllegalArgumentException ex) {
            logger.errorf("User missing parameters %s", ex.getMessage());
        } catch (WebApplicationException | IOException ex) {
            logger.errorf("Error while fetching the user license for the user with username %s. Response from server %s", user.getUsername(), ex.getMessage());
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

    private static String getSchoolIdsAttributeName(AuthenticationFlowContext context) {
        String defaultValue = "prefixedSchools";
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig == null) {
            logger.warnf("No authenticator config found, using default value %s", defaultValue);
            return defaultValue;
        }
        Map<String, String> config = authenticatorConfig.getConfig();
        if (config == null || config.isEmpty()) {
            logger.warnf("No config map inside config %s, using default value %s", authenticatorConfig.getAlias(), defaultValue);
            return defaultValue;
        }
        return config.get(LicenceConnectAuthenticatorFactory.SCHOOLIDS_ATTRIBUTE);
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

    private void populateCommonQueryParams(Map<String, String> queryParams, AuthenticationFlowContext context, UserModel user) {
        queryParams.put(BUNDESLAND_ATTRIBUTE, user.getFirstAttribute(BUNDESLAND_ATTRIBUTE));
        Optional<FederatedIdentityModel> idp = fetchFederatedIdentityModels(user, context).findFirst();
        if (idp.isPresent()) {
            String userId = idp.get().getUserId();
            queryParams.put(USER_ID, userId);
        }
    }

    private void persistUserLicense(AuthenticationFlowContext context, UserModel user, String userLicence) {
        var hmacMapper = context.getAuthenticationSession().getClient().getProtocolMappersStream()
                .filter(mapper -> HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID.equals(mapper.getProtocolMapper())).findFirst();
        LicenceEntity licence;
        if (hmacMapper.isPresent()) {
            String hmacId = HmacPairwiseSubMapperHelper.generateIdentifier(hmacMapper.get(), user);
            licence = new LicenceEntity(hmacId, userLicence);
        } else {
            licence = new LicenceEntity(user.getId(), userLicence);
        }
        context.getSession().getProvider(LicenceJpaProvider.class).persistLicence(licence);
        logger.infof("User licence has been persisted in the database for user %s", user.getUsername());
    }

    private Stream<FederatedIdentityModel> fetchFederatedIdentityModels(UserModel user, AuthenticationFlowContext context) {
        RealmModel realm = context.getRealm();
        IdentityProviderStorageProvider idpProvider = context.getSession().getProvider(IdentityProviderStorageProvider.class);
        Set<String> idps = idpProvider.getAllStream().map(IdentityProviderModel::getAlias).collect(Collectors.toSet());
        return context.getSession().users().getFederatedIdentitiesStream(realm, user)
                .filter(identity -> idps.contains(identity.getIdentityProvider()));
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
