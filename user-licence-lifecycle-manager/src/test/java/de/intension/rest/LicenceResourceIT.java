package de.intension.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.intension.rest.model.LicenceRequest;
import de.intension.testhelper.KeycloakPage;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockserver.client.MockServerClient;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.containers.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

@Testcontainers
public class LicenceResourceIT {

    private static final Network network = Network.newNetwork();
    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";
    private static final String REALM = "fwu";
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
    private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:22.0.4")
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

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static MockServerClient mockServerClient;
    private static RemoteWebDriver driver;
    private static FluentWait<WebDriver> wait;

    @BeforeAll
    static void setupAll() throws Exception {
        mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
        LicenceRequest licenceRequestedRequest = new LicenceRequest("9c7e5634-5021-4c3e-9bea-53f54c299a0f", "account-console",
                "DE-SN-Schullogin.0815", "de-DE");
        mockServerClient.when(request().withPath("/v1/licences/request")
                        .withMethod("POST")
                        .withHeader("X-API-Key", "sample-api-key")
                        .withBody(objectMapper.writeValueAsString(licenceRequestedRequest)))
                .respond(response()
                        .withStatusCode(OK_200.code())
                        .withReasonPhrase(OK_200.reasonPhrase())
                        .withBody("{\n    \"hasLicences\": true,\n    \"licences\": [\n      {\n        \"licence_code\": \"VHT-9234814-fk68-acbj6-3o9jyfilkq2pqdmxy0j\"\n      },\n      {\n        \"licence_code\": \"COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015\"\n      }\n    ]\n  }")
                        .withHeaders(header("Content-Type", "application/json")));
    }

    @BeforeEach
    void setup() throws Exception {
        driver = new RemoteWebDriver(URI.create("http://localhost:4444/wd/hub").toURL(), new FirefoxOptions());
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(5, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
    }

    @Test
    void should_return_licence_from_hmac_id() throws Exception {
        KeycloakPage.start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");
        var databaseEntry = getDatabaseEntry();
        var hmacID = databaseEntry.getLeft();
        var expectedLicence = databaseEntry.getRight();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + keycloak.getHttpPort() + "/auth/realms/fwu/licences-resource/" + hmacID)).GET().build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        var body = response.body();
        assertEquals(body, expectedLicence);
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

    @Test
    void should_return_404_when_hmac_id_does_not_exist() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + keycloak.getHttpPort() + "/auth/realms/fwu/licences-resource/1234567890")).GET().build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        assertEquals(404, response.statusCode());
    }
}
