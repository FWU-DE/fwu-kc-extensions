package de.intension.rest;

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
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.intension.rest.model.RemoveLicenseRequest;

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

    public boolean releaseLicense(RemoveLicenseRequest licenseRequest)
        throws IOException
    {
        HttpPost httpPost = new HttpPost(licenseRestUri);

        httpPost.setHeader("X-API-Key", this.licenseAPIKey);
        httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
        StringEntity entity = new StringEntity(objectMapper.writeValueAsString(licenseRequest));
        httpPost.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(httpPost);

        final int status = response.getStatusLine().getStatusCode();
        if (status == 200) {
            return true;
        }
        else {
            LOG.warnf("There was an error while releasing the license for the user. Status: %d. Reason: %s", status,
                      response.getStatusLine().getReasonPhrase());
        }
        return false;
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

    }
}
