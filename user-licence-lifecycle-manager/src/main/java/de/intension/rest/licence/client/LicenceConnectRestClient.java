package de.intension.rest.licence.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.intension.rest.licence.model.LegacyLicenceRequest;
import de.intension.rest.licence.model.LicenceRequest;
import de.intension.rest.licence.model.RemoveLicenceRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Array;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.jboss.logging.Logger.getLogger;

public class LicenceConnectRestClient
        implements Closeable {

    private static final Logger LOG = getLogger(LicenceConnectRestClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String licenceRestUri;
    private final String licenceAPIKey;

    private final CloseableHttpClient httpClient;

    public LicenceConnectRestClient(String licenceRestUri, String licenceAPIKey) {
        this.licenceRestUri = licenceRestUri;
        this.licenceAPIKey = licenceAPIKey;
        httpClient = HttpClientBuilder.create().build();
    }

    public List<String> getLicences(LicenceRequest licenceRequest) throws Exception {
        if (licenceRequest.getBundesland() == null) throw new IllegalArgumentException("Bundesland must not be null");
        if (licenceRequest.getClientName() == null) throw new IllegalArgumentException("Client name must not be null");
        var url = constructLicenseUri(licenceRequest);
        var httpGet = new HttpGet(url);
        httpGet.addHeader("X-API-KEY", this.licenceAPIKey);
        try (var response = httpClient.execute(httpGet)) {
            var status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK) {
                var jsonResponse = objectMapper.readTree(response.getEntity().getContent());
                return StreamSupport.stream(jsonResponse.spliterator(), false).map(jsonNode -> {
                    if (!jsonNode.has("licenceCode")) {
                        throw new IllegalArgumentException("Licences not found in response");
                    }
                    return jsonNode.get("licenceCode").asText();
                }).toList();
            }
        }
        throw new Exception("There was an error while fetching the licence.");
    }

    private URI constructLicenseUri(LicenceRequest licenceRequest) throws URISyntaxException {
        var urlBuilder = new URIBuilder(licenceRestUri);
        urlBuilder.addParameter("bundesland", licenceRequest.getBundesland());
        urlBuilder.addParameter("clientName", licenceRequest.getClientName());
        if (licenceRequest.getStandortnummer() != null) {
            urlBuilder.addParameter("standortnummer", licenceRequest.getStandortnummer());
        }
        if (licenceRequest.getSchulnummer() != null) {
            urlBuilder.addParameter("schulnummer", licenceRequest.getSchulnummer());
        }
        if (licenceRequest.getUserId() != null) {
            urlBuilder.addParameter("userId", licenceRequest.getUserId());
        }
        return urlBuilder.build();
    }

    @Override
    public void close()
            throws IOException {
        this.httpClient.close();
    }
}
