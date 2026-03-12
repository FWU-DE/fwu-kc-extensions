package de.intension.rest.licence.client;

import static de.intension.rest.licence.model.LicenseConstants.BUNDESLAND_ATTRIBUTE;
import static de.intension.rest.licence.model.LicenseConstants.CLIENT_ID;
import static de.intension.rest.licence.model.LicenseConstants.CLIENT_NAME;
import static de.intension.rest.licence.model.LicenseConstants.USER_ID;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.logging.Logger.getLogger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import jakarta.ws.rs.WebApplicationException;

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
        String requestString = url + "&" + String.join("?", queryParams.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toList());
        LOG.debugf("Requesting licenses with url: %s", requestString);
        SimpleHttp simpleHttp = SimpleHttp.doGet(url, session);
        addConfig(simpleHttp, queryParams);

        try (SimpleHttp.Response response = simpleHttp.asResponse()) {
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

    private void addConfig(SimpleHttp simpleHttp, Map<String,String> queryParams) {
        simpleHttp.header("X-API-KEY", this.licenceAPIKey).header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON).header(HttpHeaders.ACCEPT, APPLICATION_JSON);
        queryParams.forEach(simpleHttp::param);
    }
}
