package de.intension.resources.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.intension.testhelper.KeycloakPage;
import okhttp3.*;

class VidisAdminRealmResourceProviderIT
{

    private static final String              REALM             = "fwu";

    private static final Network             network           = Network.newNetwork();

    private static final String              IMPORT_PATH       = "/opt/keycloak/data/import/";

    private static final OkHttpClient        client            = new OkHttpClient();

    @Container
    private static final KeycloakContainer   keycloak          = new KeycloakContainer("quay.io/keycloak/keycloak:22.0.4")
        .withProviderClassesFrom("target/classes")
        .withFeaturesEnabled("admin-api")
        .withContextPath("/auth")
        .withNetwork(network)
        .withNetworkAliases("test")
        .withClasspathResourceMapping("fwu-realm.json", IMPORT_PATH + "fwu-realm.json", BindMode.READ_ONLY)
        .withClasspathResourceMapping("idp-realm.json", IMPORT_PATH + "idp-realm.json", BindMode.READ_ONLY)
        .withRealmImportFiles("/fwu-realm.json", "/idp-realm.json")
//        .withDebugFixedPort(8787, true)
        .withAccessToHost(true);

    private static final GenericContainer<?> firefoxStandalone = new GenericContainer<>(DockerImageName.parse("selenium/standalone-firefox:4.3.0-20220706"))
        .withNetwork(network)
        .withNetworkAliases("test")
        .withExposedPorts(4444, 5900)
        .withSharedMemorySize(2000000000L);

    private RemoteWebDriver                  driver;
    private FluentWait<WebDriver>            wait;

    public static final String               ADMIN_USERNAME    = keycloak.getAdminUsername();
    public static final String               ADMIN_PASSWORD    = keycloak.getAdminPassword();

    @Test
    void shouldDeleteUsersWithCreatedTimestam_whenAllConfigured()
        throws IOException
    {
        keycloak.withEnv("KC_SPI_ADMIN_REALM_RESTAPI_EXTENSION_VIDIS_CUSTOM_FWU", "ALL");
        keycloak.start();
        UserRepresentation user = new UserRepresentation();
        user.setEmail("test@intension.de");
        user.setUsername("test");
        keycloak.getKeycloakAdminClient().realm("fwu").users().create(user);

        Integer userCountBeforeCleanup = keycloak.getKeycloakAdminClient().realm("fwu").users().count();
        String authServerUrl = keycloak.getAuthServerUrl();
        String accessToken = getAccessToken(authServerUrl + "/realms/master/protocol/openid-connect/token", ADMIN_USERNAME, ADMIN_PASSWORD);

        Integer deletedUsers = deleteUsers(accessToken, authServerUrl);
        Integer userCountAfterCleanup = keycloak.getKeycloakAdminClient().realm("fwu").users().count();

        assertThat(deletedUsers).as("Deleted users").isPositive();
        assertThat(userCountBeforeCleanup).as("Users before cleanup").isGreaterThan(userCountAfterCleanup);
        assertThat(userCountAfterCleanup).as("Users after cleanup")
            .isEqualTo(userCountBeforeCleanup - deletedUsers)
            .isPositive();
    }

    @Test
    void shouldDeleteIdpUsers_whenIdpConfigured()
        throws IOException
    {
        keycloak.withEnv("KC_SPI_ADMIN_REALM_RESTAPI_EXTENSION_VIDIS_CUSTOM_FWU", "IDP");
        keycloak.start();

        KeycloakPage.start(driver, wait).openAccountConsole().idpLogin("idpuser", "test");
        RealmResource realm = keycloak.getKeycloakAdminClient().realms().realm("fwu");
        String userId = realm.users().search("idpuser").get(0).getId();
        realm.users().get(userId).getUserSessions().forEach(session -> realm.deleteSession(session.getId(), false));

        Integer userCountBeforeCleanup = realm.users().count();
        String authServerUrl = keycloak.getAuthServerUrl();
        String accessToken = getAccessToken(authServerUrl + "/realms/master/protocol/openid-connect/token", ADMIN_USERNAME, ADMIN_PASSWORD);

        Integer deletedUsers = deleteUsers(accessToken, authServerUrl);
        Integer userCountAfterCleanup = keycloak.getKeycloakAdminClient().realm("fwu").users().count();

        assertThat(deletedUsers).as("Should have deleted IDP Users").isPositive();
        assertThat(userCountBeforeCleanup).as("Usercount should be different after deletion").isGreaterThan(userCountAfterCleanup);
        assertThat(userCountAfterCleanup).as("Usercount after cleanup").isEqualTo(userCountBeforeCleanup - deletedUsers).isPositive();
    }

    private String getAccessToken(String tokenUrl, String username, String password)
        throws IOException
    {
        RequestBody formBody = new FormBody.Builder()
            .add("grant_type", "password")
            .add("client_id", "admin-cli")
            .add("username", username)
            .add("password", password)
            .build();

        Request request = new Request.Builder()
            .url(tokenUrl)
            .post(formBody)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            Map<String, String> responseBody = JsonSerialization.readValue(response.body().string(), Map.class);
            return responseBody.get("access_token");
        }
    }

    private Integer deleteUsers(String accessToken, String authServerUrl)
        throws IOException
    {
        Request request = new Request.Builder()
            .url(authServerUrl + "/admin/realms/fwu/vidis-custom/users/inactive?max=1000")
            .delete()
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to cleanUp users. got http: " + response);
            }
            Map<String, Integer> responseBody = JsonSerialization.readValue(response.body().string(), Map.class);
            return responseBody.get("deletedUsers");
        }
    }

    @BeforeAll
    static void startContainers()
    {
        firefoxStandalone.start();
    }

    @BeforeEach
    void setupSelenium()
        throws MalformedURLException
    {
        FirefoxOptions fOptions = new FirefoxOptions();
        driver = new RemoteWebDriver(new URL("http://localhost:" + firefoxStandalone.getMappedPort(4444) + "/wd/hub"), fOptions);
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(5, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
    }

    @AfterAll
    static void stopContainers()
    {
        firefoxStandalone.stop();
    }

    @AfterEach
    void cleanUp()
    {
        driver.quit();
        keycloak.stop();
    }
}