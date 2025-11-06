package de.intension.listener;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.intension.testhelper.KeycloakPage;
import de.intension.testhelper.LicenceMockHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.mockserver.client.MockServerClient;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class RemoveIdpUserOnLogOutEventIT {

    private static final String KEYCLOAK_VERSION = System.getProperty("keycloak.version", "latest");
    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";
    private static final String REALM = "fwu";

    private static final Network network = Network.newNetwork();
    private static final Capabilities capabilities = new FirefoxOptions();

    @Container
    private static final MockServerContainer mockServer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.13.2"))
            .withNetwork(network)
            .withNetworkAliases("mockserver");

    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer(String.format("quay.io/keycloak/keycloak:%s", KEYCLOAK_VERSION))
            .withProviderClassesFrom("target/classes")
            .withProviderLibsFrom(List.of(new File("../target/hmac-mapper.jar")))
            .withContextPath("/auth")
            .withNetwork(network)
            .withNetworkAliases("test")
            .withClasspathResourceMapping("fwu-realm.json", IMPORT_PATH + "fwu-realm.json", BindMode.READ_ONLY)
            .withClasspathResourceMapping("idp-realm.json", IMPORT_PATH + "idp-realm.json", BindMode.READ_ONLY)
            .withRealmImportFiles("/fwu-realm.json", "/idp-realm.json")
            .withEnv("KC_SPI_EVENTS_LISTENER_REMOVE_USER_ON_LOGOUT_FWU", "IDP")
            .withEnv("KC_SPI_REST_CLIENT_DEFAULT_LICENCE_CONNECT_BASE_URL", "http://mockserver:1080")
            .withEnv("KC_SPI_REST_CLIENT_DEFAULT_LICENCE_CONNECT_API_KEY", "sample-api-key")
            .dependsOn(mockServer);

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
    void setup()
            throws Exception {
        driver = new RemoteWebDriver(selenium.getSeleniumAddress(), capabilities, false);
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(5, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
        LicenceMockHelper.requestLicenceExpectation(mockServerClient);
    }

    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user logout
     * THEN: user is removed from the Keycloak
     */
    @Test
    void should_remove_user_on_logout_for_identity_provider_login() throws IOException, InterruptedException {

        String tokenUrl = keycloak.getAuthServerUrl() + "/realms/master/protocol/openid-connect/token";

        // Build form data
        String formData = "client_id=" + URLEncoder.encode("admin-cli", StandardCharsets.UTF_8)
            + "&username=" + URLEncoder.encode("admin", StandardCharsets.UTF_8)
            + "&password=" + URLEncoder.encode("keycloak", StandardCharsets.UTF_8)
            + "&grant_type=" + URLEncoder.encode("password", StandardCharsets.UTF_8);

        // Create HTTP client and request
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formData))
            .build();

        // Send request
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        System.out.println("Response: " + response.body());

        assertEquals(200, response.statusCode());







        assertTrue(keycloak.isRunning());
//        System.out.println(keycloak.getKeycloakAdminClient().serverInfo().getInfo());
        Keycloak adminClient = KeycloakBuilder.builder()
            .serverUrl(keycloak.getAuthServerUrl())
            .realm("master")
            .clientId("admin-cli")
            .username(keycloak.getAdminUsername())
            .password(keycloak.getAdminPassword())
            .build();
        System.out.println(adminClient.serverInfo().getInfo());


        KeycloakPage kcPage = KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");

        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();

        int usersCountBeforeLogout = usersResource.count();

        kcPage.logout();

        int usersCountAfterLogout = usersResource.count();
        assertEquals(usersCountBeforeLogout - 1, usersCountAfterLogout);
    }

    /**
     * GIVEN: a user login without identity federation
     * WHEN: the same user logout
     * THEN: user is not removed from the Keycloak
     */
    @Test
    void should_not_remove_user_on_logout_for_non_identity_provider_login() {
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage kcPage = KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                .login("misty", "test");
        int usersCountBeforeLogout = usersResource.count();

        kcPage.logout();

        int usersCountAfterLogout = usersResource.count();
        assertEquals(usersCountBeforeLogout, usersCountAfterLogout);
    }

    /**
     * GIVEN: a user login with identity federation
     * WHEN: the user's last name is updated to generate UPDATE_PROFILE event
     * THEN: user is not removed from the Keycloak
     */
    @Test
    void should_not_remove_user_when_other_event_than_logout() {
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage kcPage = KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");
        int usersCountBeforeLogout = usersResource.count();

        kcPage.updateLastName("userTest");

        int usersCountAfterLogout = usersResource.count();
        assertEquals(usersCountBeforeLogout, usersCountAfterLogout);
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }
}
