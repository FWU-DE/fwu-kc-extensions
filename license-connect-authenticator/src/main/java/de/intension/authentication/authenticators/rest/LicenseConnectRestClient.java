package de.intension.authentication.authenticators.rest;

import static org.jboss.logging.Logger.getLogger;

import java.io.Closeable;
import java.io.IOException;

import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intension.authentication.authenticators.rest.model.LicenseRequest;

public class LicenseConnectRestClient
    implements Closeable
{

    private static final Logger LOG          = getLogger(LicenseConnectRestClient.class);
    private final ObjectMapper  objectMapper = new ObjectMapper();
    private final String        licenseRestUri;
    private final String        licenseAPIKey;

    private CloseableHttpClient httpClient;

    public LicenseConnectRestClient(String licenseRestUri, String licenseAPIKey)
    {
        this.licenseRestUri = licenseRestUri;
        this.licenseAPIKey = licenseAPIKey;
        httpClient = HttpClientBuilder.create().setDefaultRequestConfig(getRequestConfig()).build();
    }

    public JsonNode getLicenses(LicenseRequest licenseRequest)
        throws IOException
    {
        JsonNode userLicenses = null;
        HttpPost httpPost = new HttpPost(licenseRestUri);

        httpPost.setHeader("X-API-Key", this.licenseAPIKey);
        httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
        StringEntity entity = new StringEntity(objectMapper.writeValueAsString(licenseRequest));
        httpPost.setEntity(entity);

        try (CloseableHttpResponse response = httpClient.execute(httpPost);) {
            final int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                try {
                    userLicenses = objectMapper.readTree(EntityUtils.toString(response.getEntity()));
                } catch (JsonProcessingException e) {
                    LOG.error("Error while parsing user licenses ", e);
                }
            }
            else {
                LOG.warnf("There was an error while fetching the license for the user. Status: %d. Reason: %s", status,
                          response.getStatusLine().getReasonPhrase());
            }
        }
        return userLicenses;
    }

    /**
     * Get request configuration for timeout handling.
     */
    private static RequestConfig getRequestConfig()
    {
        int timeoutInSeconds = 10;
        return RequestConfig.custom()
            .setConnectTimeout(timeoutInSeconds * 1000)
            .setConnectionRequestTimeout(timeoutInSeconds * 1000)
            .setSocketTimeout(timeoutInSeconds * 1000).build();
    }

    @Override
    public void close()
        throws IOException
    {
        this.httpClient.close();
    }
}