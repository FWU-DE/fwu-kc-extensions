package de.intension.testhelper;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@SuppressWarnings("unchecked")
public class HttpClientHelper {

    public static String getAccessToken(HttpClient client, String tokenUrl, String username, String password)
            throws IOException, InterruptedException {
        String formBody = "grant_type=" + URLEncoder.encode("password", StandardCharsets.UTF_8)
                + "&client_id=" + URLEncoder.encode("admin-cli", StandardCharsets.UTF_8)
                + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Unexpected code " + response.statusCode());
        }

        Map<String, String> responseBody = JsonSerialization.readValue(response.body(), Map.class);
        return responseBody.get("access_token");
    }

    public static Integer deleteUsers(HttpClient client, String accessToken, String authServerUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authServerUrl + "/admin/realms/fwu/vidis-custom/users/inactive?max=1000"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to cleanUp users. got http: " + response.statusCode());
        }

        Map<String, Integer> responseBody = JsonSerialization.readValue(response.body(), Map.class);
        return responseBody.get("deletedUsers");
    }
}
