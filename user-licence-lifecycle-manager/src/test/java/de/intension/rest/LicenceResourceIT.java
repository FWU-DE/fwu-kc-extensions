package de.intension.rest;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.intension.testhelper.KeycloakPage;
import de.intension.testhelper.LicenceMockHelper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class LicenceResourceIT {

    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";

    private static final Network network = Network.newNetwork();
    private static final Capabilities capabilities = new FirefoxOptions();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

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
        driver = new RemoteWebDriver(selenium.getSeleniumAddress(), capabilities);
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(5, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
    }

    @Test
    void should_return_licence_from_hmac_id() throws Exception {
        LicenceMockHelper.requestLicenceExpectation(mockServerClient);
        KeycloakPage.start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");
        var databaseEntry = getDatabaseEntry();
        var hmacID = databaseEntry.getLeft();
        var expectedLicence = databaseEntry.getRight();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + keycloak.getHttpPort() + "/auth/realms/fwu/licences/" + hmacID)).GET().build();
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
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + keycloak.getHttpPort() + "/auth/realms/fwu/licences/invalid-hmac-id")).GET().build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        assertEquals(404, response.statusCode());
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }
}
