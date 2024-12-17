package de.intension.resources.admin;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.mock.Expectation;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.intension.rest.model.RemoveLicenceRequest;
import de.intension.testhelper.KeycloakPage;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class VidisAdminRealmResourceProviderIT
{

    private static final String              REALM             = "fwu";

    private static final Network             network           = Network.newNetwork();

    private static final String              IMPORT_PATH       = "/opt/keycloak/data/import/";

    private static final OkHttpClient        client            = new OkHttpClient();

    private final ObjectMapper               objectMapper      = new ObjectMapper();

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

    private static final MockServerContainer mockServer        = new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.13.2"))
        .withNetwork(network)
        .withNetworkAliases("mockserver")
        .withExposedPorts(1080, 1090);

    private RemoteWebDriver                  driver;
    private FluentWait<WebDriver>            wait;

    public static final String               ADMIN_USERNAME    = keycloak.getAdminUsername();
    public static final String               ADMIN_PASSWORD    = keycloak.getAdminPassword();

    @Test
    void shouldDeleteUsersWithCreatedTimestamp_whenAllConfigured()
        throws IOException
    {
        keycloak.withEnv("KC_SPI_ADMIN_REALM_RESTAPI_EXTENSION_VIDIS_CUSTOM_FWU", "ALL");
        keycloak.start();
        UserRepresentation user = new UserRepresentation();
        user.setId("sampleUserId");
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
        try (
                MockServerClient mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())) {
            Expectation releaseLicence = releaseLicenceExpectation(mockServerClient);
            keycloak.withEnv("KC_SPI_ADMIN_REALM_RESTAPI_EXTENSION_VIDIS_CUSTOM_FWU", "IDP");
            keycloak.withEnv("KC_SPI_REMOVE_USER_REST_CLIENT_DEFAULT_LICENCE_CONNECT_API_KEY", "sample-api-key");
            keycloak.withEnv("KC_SPI_REMOVE_USER_REST_CLIENT_DEFAULT_LICENCE_CONNECT_BASE_URL", "http://mockserver:1080/v1/licences/release");
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
            mockServerClient.verify(releaseLicence.getId(), VerificationTimes.once());
        }
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

    private Expectation releaseLicenceExpectation(MockServerClient clientAndServer)
        throws JsonProcessingException
    {
        RemoveLicenceRequest licenceRequestedRequest = new RemoveLicenceRequest("9c7e5634-5021-4c3e-9bea-53f54c299a0f");
        return clientAndServer
            .when(
                  request().withPath("/v1/licences/release")
                      .withMethod("POST")
                      .withHeader("X-API-Key", "sample-api-key")
                      .withBody(objectMapper.writeValueAsString(licenceRequestedRequest)),
                  Times.exactly(1))
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withHeaders(
                                      header(CONTENT_TYPE.toString(), MediaType.JSON_UTF_8.getType())))[0];
    }

    @BeforeAll
    static void startContainers()
    {
        firefoxStandalone.start();
        mockServer.start();
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