package de.intension.rest.licence.client;

import jakarta.ws.rs.WebApplicationException;
import org.apache.http.HttpHeaders;
import org.jboss.logging.Logger;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static de.intension.rest.licence.model.LicenseConstants.*;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.logging.Logger.getLogger;

public class LicenceConnectRestClient {

    private static final Logger LOG                = getLogger(LicenceConnectRestClient.class);
    private final List<String>    biloRequiredParams      = List.of(USER_ID, CLIENT_ID);
    private final List<String>  genericLcRequiredParams = List.of(CLIENT_NAME, BUNDESLAND_ATTRIBUTE);
    private static final String   UCS_REQUEST_PATH        = "v1/bilo/request";
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

        String url = String.format("%s/%s/%s", licenceRestUri, UCS_REQUEST_PATH, queryParams.get(USER_NAME));
        String requestString = url + "?clientName" + queryParams.get(CLIENT_ID);
        LOG.debugf("Requesting bilo licenses with url: %s", requestString);
        SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doGet(url);
        addConfig(simpleHttp, Map.of("clientName", queryParams.get(CLIENT_ID)));

        try (SimpleHttpResponse response = simpleHttp.asResponse()) {
            if (response.getStatus() == 200) {
                LOG.debugf("Received success response for the user for the license type bilo");
                String responseString = response.asString();
                LOG.debugf("Response content length: %d starting with %s", responseString.length(),
                    responseString.substring(0, Math.min(100, responseString.length())));
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
        String requestString = url + "?" + String.join("&", queryParams.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
        LOG.debugf("Requesting licenses with url: %s", requestString);
        SimpleHttpRequest simpleHttp = SimpleHttp.create(session).doGet(url);
        addConfig(simpleHttp, queryParams);

        try (SimpleHttpResponse response = simpleHttp.asResponse()) {
            if (response.getStatus() == 200) {
                LOG.debugf("Received success response for the user for the license type LC");
                String responseString = response.asString();
                LOG.debugf("Response content length: %d starting with %s", responseString.length(),
                    responseString.substring(0, Math.min(100, responseString.length())));
                return responseString;
            }
            throw new WebApplicationException(response.getStatus());
        }
    }

    private void addConfig(SimpleHttpRequest simpleHttp, Map<String,String> queryParams) {
        simpleHttp.header("X-API-KEY", this.licenceAPIKey).header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).header(HttpHeaders.ACCEPT, APPLICATION_JSON);
        queryParams.forEach(simpleHttp::param);
    }
}
