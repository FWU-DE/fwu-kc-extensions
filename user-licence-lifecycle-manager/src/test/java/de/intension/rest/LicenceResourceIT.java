package de.intension.rest;

import de.intension.keycloak.IntensionKeycloakContainer;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static jakarta.ws.rs.core.Response.Status.OK;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.OAuth2Constants.*;

@Testcontainers
public class LicenceResourceIT {

    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";

    private static final Network network = Network.newNetwork();
    private static final Capabilities capabilities = new FirefoxOptions();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("keycloak")
            .withUsername("keycloak")
            .withPassword("test123");

    @Container
    private static final MockServerContainer mockServer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.13.2"))
            .withNetwork(network)
            .withNetworkAliases("mockserver");

    @Container
    private static final IntensionKeycloakContainer keycloak = new IntensionKeycloakContainer()
            .withProviderClassesFrom("target/classes")
            .withProviderLibsFrom(List.of(new File("../target/hmac-mapper.jar")))
            .withContextPath("/auth")
            .withNetwork(network)
            .withNetworkAliases("test")
            .withClasspathResourceMapping("fwu-realm.json", IMPORT_PATH + "fwu-realm.json", BindMode.READ_ONLY)
            .withClasspathResourceMapping("idp-realm.json", IMPORT_PATH + "idp-realm.json", BindMode.READ_ONLY)
            .withRealmImportFiles("/fwu-realm.json", "/idp-realm.json")
            .withEnv("KC_SPI_AUTHENTICATOR_LICENCE_CONNECT_AUTHENTICATOR_LICENCE_URL", "http://mockserver:1080/v1/licences/request")
            .withEnv("KC_SPI_AUTHENTICATOR_LICENCE_CONNECT_AUTHENTICATOR_LICENCE_API_KEY", "sample-api-key")
            .withEnv("KC_DB", "postgres")
            .withEnv("KC_DB_URL_HOST", "postgres")
            .withEnv("KC_DB_USERNAME", "keycloak")
            .withEnv("KC_DB_PASSWORD", "test123")
            .dependsOn(postgres, mockServer);

    @Container
    private static final BrowserWebDriverContainer<?> selenium = new BrowserWebDriverContainer<>()
            .withCapabilities(capabilities)
            .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null)
            .withNetwork(network);

    private static MockServerClient mockServerClient;

    private RemoteWebDriver driver;
    private FluentWait<WebDriver> wait;

    @BeforeAll
    static void setupAll() {
        mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
    }

    @BeforeEach
    void setup() {
        driver = new RemoteWebDriver(selenium.getSeleniumAddress(), capabilities, false);
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(5, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
    }

//    @Test
//    void should_return_licence_from_hmac_id() throws Exception {
//        LicenceMockHelper.requestLicenceExpectation(mockServerClient);
//        KeycloakPage.start(driver, wait)
//                .openAccountConsole()
//                .idpLogin("idpuser", "test");
//        var databaseEntry = getDatabaseEntry();
//        var hmacID = databaseEntry.getLeft();
//        var expectedLicence = databaseEntry.getRight();
//        var accessToken = getAccessToken();
//        var request = HttpRequest.newBuilder(URI.create(keycloak.getAuthServerUrl() + "/realms/fwu/licences/" + hmacID)).GET().header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).build();
//        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//        assertEquals(OK.getStatusCode(), response.statusCode());
//        var body = response.body();
//        assertEquals(body, expectedLicence);
//    }

    @Test
    void should_return_200_with_empty_json_object_when_hmac_id_does_not_exist() throws Exception {
        var accessToken = getAccessToken();
        var request = HttpRequest.newBuilder(URI.create(keycloak.getAuthServerUrl() + "/realms/fwu/licences/invalid-hmac-id")).GET().header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(OK.getStatusCode(), response.statusCode());
        assertEquals("{}", response.body());
    }

    @Test
    void should_return_401_when_no_access_token_is_provided() throws Exception {
        var request = HttpRequest.newBuilder(URI.create(keycloak.getAuthServerUrl() + "/realms/fwu/licences/ignored")).GET().build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(UNAUTHORIZED.getStatusCode(), response.statusCode());
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    private Pair<String, String> getDatabaseEntry() throws Exception {
        var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        try (var statement = connection.prepareStatement("SELECT * FROM Licence;");
             var resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return Pair.of(resultSet.getString(1), resultSet.getString(2));
            }
        }
        throw new Exception("No licence found in database!");
    }

    private String getAccessToken() throws Exception {
        var parameters = GRANT_TYPE + "=" + PASSWORD + "&" + USERNAME + "=misty&" + PASSWORD + "=test&" + CLIENT_ID + "=admin-cli";
        var request = HttpRequest.newBuilder(URI.create(keycloak.getAuthServerUrl() + "/realms/fwu/protocol/openid-connect/token"))
                .POST(HttpRequest.BodyPublishers.ofString(parameters))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        var json = objectMapper.readTree(response.body());
        return json.get("access_token").asText();
    }
}
