package de.intension.authentication.authenticators.license.rest;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class LicenceResourceTest {

    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:22.0.4")
            .withProviderClassesFrom("target/classes")
            .withRealmImportFile("/licence-test-realm.json")
            .withEnv("KC_SPI_REALM_RESTAPI_EXTENSION_LICENCE_RESOURCE_REALM_NAME", "test")
            .withDebugFixedPort(8787, false);

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void should_get_licence_from_user() throws Exception {
        var response = sendRequest("9c7e5634-5021-4c3e-9bea-53f54c299a0f");

        assertEquals(200, response.statusCode());
        assertEquals("""
                {
                  "key1" : "value1",
                  "key2" : true,
                  "key3" : 1234
                }""", response.body());
    }

    @Test
    void should_not_get_licence_from_user_when_license_is_invalid() throws Exception {
        var response = sendRequest("5e40ef11-d3e4-46cb-aefc-3b390f19ba21");

        assertEquals(500, response.statusCode());
    }

    @Test
    void should_not_get_licence_from_user_when_license_part_is_missing() throws Exception {
        var response = sendRequest("e12ed580-ee21-4df0-9275-6d5588db08af");

        assertEquals(500, response.statusCode());
    }

    @Test
    void should_return_not_found_when_user_id_is_non_existent() throws Exception {
        var response = sendRequest("yeet");

        assertEquals(404, response.statusCode());
    }

    private HttpResponse<String> sendRequest(String userID) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://" + keycloak.getHost() + ":" + keycloak.getHttpPort() + "/realms/test/licence-resource/" + userID)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
