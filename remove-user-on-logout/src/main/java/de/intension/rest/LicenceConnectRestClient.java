package de.intension.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.intension.rest.model.RemoveLicenceRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.logging.Logger;

import java.io.Closeable;
import java.io.IOException;

import static org.jboss.logging.Logger.getLogger;

public class LicenceConnectRestClient
    implements Closeable
{

    private static final Logger LOG          = getLogger(LicenceConnectRestClient.class);
    private final ObjectMapper  objectMapper = new ObjectMapper();
    private final String        licenceRestUri;
    private final String        licenceAPIKey;

    private CloseableHttpClient httpClient;

    public LicenceConnectRestClient(String licenceRestUri, String licenceAPIKey)
    {
        this.licenceRestUri = licenceRestUri;
        this.licenceAPIKey = licenceAPIKey;
        httpClient = HttpClientBuilder.create().setDefaultRequestConfig(getRequestConfig()).build();
    }

    public boolean releaseLicence(RemoveLicenceRequest licenceRequest)
        throws IOException
    {
        HttpPost httpPost = new HttpPost(licenceRestUri);

        httpPost.setHeader("X-API-Key", this.licenceAPIKey);
        httpPost.setHeader(HttpHeaders.ACCEPT, "application/json");
        StringEntity entity = new StringEntity(objectMapper.writeValueAsString(licenceRequest));
        httpPost.setEntity(entity);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            final int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                return true;
            }
            else {
                LOG.warnf("There was an error while releasing the licence for the user. Status: %d. Reason: %s", status,
                          response.getStatusLine().getReasonPhrase());
            }
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
