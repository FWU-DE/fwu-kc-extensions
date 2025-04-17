package de.intension.rest.licence.client;

import jakarta.ws.rs.WebApplicationException;
import org.apache.http.HttpHeaders;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static de.intension.rest.licence.model.LicenseConstants.*;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.logging.Logger.getLogger;

public class LicenceConnectRestClient {

    private static final Logger LOG                = getLogger(LicenceConnectRestClient.class);
    private final List<String>  biloRequiredParams      = List.of(USER_ID, CLIENT_ID, BUNDESLAND_ATTRIBUTE);
    private final List<String>  genericLcRequiredParams = List.of(CLIENT_NAME, BUNDESLAND_ATTRIBUTE);
    private static final String UCS_REQUEST_PATH        = "v1/ucs/request";
    private static final String LC_REQUEST_PATH = "v1/licences/request";
    private final String licenceRestUri;
    private final String licenceAPIKey;
    private final KeycloakSession session;


    public LicenceConnectRestClient(KeycloakSession session, String licenceRestUri, String licenceAPIKey) {
        this.licenceRestUri = licenceRestUri;
        this.licenceAPIKey = licenceAPIKey;
        this.session = session;
    }

    public String getUcsLicences(Map<String,String> queryParams)
            throws IOException
    {
        List<String> missing = biloRequiredParams.stream()
                .filter(param -> queryParams.get(param) == null)
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameters: " + String.join(", ", missing));
        }

        String url = String.format("%s/%s", licenceRestUri, UCS_REQUEST_PATH);
        SimpleHttp simpleHttp = SimpleHttp.doGet(url, session);
        addConfig(simpleHttp, queryParams);

        try (SimpleHttp.Response response = simpleHttp.asResponse()) {
            if (response.getStatus() == 200) {
                LOG.debugf("Received success response for the user for the license type UCS");
                return response.asString();
            }
            throw new WebApplicationException(response.getStatus());
        }
    }

    public String getLicences(Map<String,String> queryParams)
            throws IOException
    {
        List<String> missing = genericLcRequiredParams.stream()
                .filter(param -> queryParams.get(param) == null)
                .toList();

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameters: " + String.join(", ", missing));
        }

        String url = String.format("%s/%s", licenceRestUri, LC_REQUEST_PATH);
        SimpleHttp simpleHttp = SimpleHttp.doGet(url, session);
        addConfig(simpleHttp, queryParams);

        try (SimpleHttp.Response response = simpleHttp.asResponse()) {
            if (response.getStatus() == 200) {
                LOG.debugf("Received success response for the user for the license type LC");
                return response.asString();
            }
            throw new WebApplicationException(response.getStatus());
        }
    }

    private void addConfig(SimpleHttp simpleHttp, Map<String,String> queryParams) {
        simpleHttp.header("X-API-KEY", this.licenceAPIKey).header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).header(HttpHeaders.ACCEPT, APPLICATION_JSON);
        queryParams.forEach(simpleHttp::param);
    }
}
