package de.intension.authentication.rest;

import static org.jboss.logging.Logger.getLogger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.utils.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import twitter4j.JSONException;
import twitter4j.JSONObject;

public class SchoolAssignmentsClient
{

    private static final Logger LOG          = getLogger(SchoolAssignmentsClient.class);
    private final ObjectMapper  objectMapper = new ObjectMapper();
    private String              restApiUrl;
    private String              kcAuthUrl;
    private static final RequestConfig requestConfig = getRequestConfig(10);

    public SchoolAssignmentsClient(String kcAuthUrl, String restApiUrl)
    {
        this.restApiUrl = restApiUrl;
        this.kcAuthUrl = kcAuthUrl;
    }

    /**
     * Get REST-API url.
     */
    public String getUrl()
    {
        return restApiUrl;
    }

    /**
     * Get list of allowed schools for a combination of Identity-Provider and Service-Provider
     */
    public SchoolConfigDTO getListOfAllowedSchools(String idp, String clientId, String apiRealm, String apiClientId, String apiClientSecret)
        throws JSONException, IOException, URISyntaxException
    {
        SchoolConfigDTO schoolConfig = null;
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
        HttpGet httpGet = new HttpGet(restApiUrl);
        URI uri = new URIBuilder(httpGet.getURI())
            .addParameter("serviceProvider", clientId)
            .addParameter("idpId", idp)
            .build();
        httpGet.setURI(uri);

        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken(apiRealm, apiClientId, apiClientSecret));
        httpGet.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        CloseableHttpResponse response = httpClient.execute(httpGet);

        final int status = response.getStatusLine().getStatusCode();
        if (status == 200) {
            LOG.debugv("Whitelist entries found for clientId [{0}]", clientId);
            try {
                schoolConfig = objectMapper.readValue(EntityUtils.toString(response.getEntity()), SchoolConfigDTO.class);
            } catch (JsonProcessingException e) {
                LOG.error("Error while parsing whitelist entries for clientId [{0}]", clientId, e);
            }
        }
        else {
            LOG.debugv("No Whitelist entries found for clientId [{0}]", clientId);
        }
        return schoolConfig;
    }

    /**
     * Calls the token endpoint to get a valid token that can be used to send the events to IMS.
     */
    private String getAccessToken(String realm, String clientId, String clientSecret)
        throws JSONException, IOException
    {
        CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", kcAuthUrl, realm);

        HttpPost httpPost = new HttpPost(tokenUrl);
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        httpPost.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CLIENT_CREDENTIALS));
        params.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
        params.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, clientSecret));

        httpPost.setEntity(new UrlEncodedFormEntity(params));

        CloseableHttpResponse response = httpClient.execute(httpPost);

        LOG.debugv("Get access token. Status = {0, number, integer}", response.getStatusLine().getStatusCode());

        JSONObject readEntity = new JSONObject(EntityUtils.toString(response.getEntity()));

        response.close();
        httpClient.close();

        return readEntity.getString(OAuth2Constants.ACCESS_TOKEN);
    }

    /**
     * Get request configuration for timeout handling.
     */
    private static RequestConfig getRequestConfig(int timeoutInSeconds)
    {
        return RequestConfig.custom()
                            .setConnectTimeout(timeoutInSeconds * 1000)
                            .setConnectionRequestTimeout(timeoutInSeconds * 1000)
                            .setSocketTimeout(timeoutInSeconds * 1000).build();
    }

}
