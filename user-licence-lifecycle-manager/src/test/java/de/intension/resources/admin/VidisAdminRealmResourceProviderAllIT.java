package de.intension.resources.admin;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import okhttp3.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class VidisAdminRealmResourceProviderAllIT {

    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";

    private static final Network network = Network.newNetwork();
    private static final Capabilities capabilities = new FirefoxOptions();
    private static final OkHttpClient client = new OkHttpClient();

    @Container
    private static final MockServerContainer mockServer = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.13.2"))
            .withNetwork(network)
            .withNetworkAliases("mockserver");

    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.4.2")
            .withProviderClassesFrom("target/classes")
            .withProviderLibsFrom(List.of(new File("../target/hmac-mapper.jar")))
            .withFeaturesEnabled("admin-api")
            .withContextPath("/auth")
            .withNetwork(network)
            .withNetworkAliases("test")
            .withClasspathResourceMapping("fwu-realm.json", IMPORT_PATH + "fwu-realm.json", BindMode.READ_ONLY)
            .withClasspathResourceMapping("idp-realm.json", IMPORT_PATH + "idp-realm.json", BindMode.READ_ONLY)
            .withRealmImportFiles("/fwu-realm.json", "/idp-realm.json")
            .withEnv("KC_SPI_ADMIN_REALM_RESTAPI_EXTENSION_VIDIS_CUSTOM_FWU", "ALL")
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
    void setup() {
        driver = new RemoteWebDriver(selenium.getSeleniumAddress(), capabilities);
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(5, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
    }

    @Test
    void shouldDeleteUsersWithCreatedTimestamp_whenAllConfigured() throws IOException {
        UserRepresentation user = new UserRepresentation();
        user.setId("sampleUserId");
        user.setEmail("test@intension.de");
        user.setUsername("test");
        keycloak.getKeycloakAdminClient().realm("fwu").users().create(user);

        Integer userCountBeforeCleanup = keycloak.getKeycloakAdminClient().realm("fwu").users().count();
        String authServerUrl = keycloak.getAuthServerUrl();
        String accessToken = getAccessToken(authServerUrl + "/realms/master/protocol/openid-connect/token", keycloak.getAdminUsername(), keycloak.getAdminPassword());

        Integer deletedUsers = deleteUsers(accessToken, authServerUrl);
        Integer userCountAfterCleanup = keycloak.getKeycloakAdminClient().realm("fwu").users().count();

        assertThat(deletedUsers).as("Deleted users").isPositive();
        assertThat(userCountBeforeCleanup).as("Users before cleanup").isGreaterThan(userCountAfterCleanup);
        assertThat(userCountAfterCleanup).as("Users after cleanup").isEqualTo(userCountBeforeCleanup - deletedUsers).isPositive();

    }

    private String getAccessToken(String tokenUrl, String username, String password) throws IOException {
        RequestBody formBody = new FormBody.Builder().add("grant_type", "password").add("client_id", "admin-cli").add("username", username).add("password", password).build();

        Request request = new Request.Builder().url(tokenUrl).post(formBody).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            Map<String, String> responseBody = JsonSerialization.readValue(response.body().string(), Map.class);
            return responseBody.get("access_token");
        }
    }

    private Integer deleteUsers(String accessToken, String authServerUrl) throws IOException {
        Request request = new Request.Builder().url(authServerUrl + "/admin/realms/fwu/vidis-custom/users/inactive?max=1000").delete().addHeader("Authorization", "Bearer " + accessToken).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to cleanUp users. got http: " + response);
            }
            Map<String, Integer> responseBody = JsonSerialization.readValue(response.body().string(), Map.class);
            return responseBody.get("deletedUsers");
        }
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }
}
