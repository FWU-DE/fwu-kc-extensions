package de.intension.listener;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.resource.UsersResource;
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
import de.intension.rest.model.RemoveLicenseRequest;
import de.intension.testhelper.KeycloakPage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReleaseLicenseOnLogOutEventIT
{

    private static final Network             network           = Network.newNetwork();
    private static final String              IMPORT_PATH       = "/opt/keycloak/data/import/";
    private static final String              REALM             = "fwu";
    private final ObjectMapper               objectMapper      = new ObjectMapper();

    @Container
    private static final KeycloakContainer   keycloak          = new KeycloakContainer("quay.io/keycloak/keycloak:22.0.4")
        .withProviderClassesFrom("target/classes")
        .withContextPath("/auth")
        .withNetwork(network)
        .withNetworkAliases("test")
        .withClasspathResourceMapping("fwu-realm.json", IMPORT_PATH + "fwu-realm.json", BindMode.READ_ONLY)
        .withClasspathResourceMapping("idp-realm.json", IMPORT_PATH + "idp-realm.json", BindMode.READ_ONLY)
        .withRealmImportFiles("/fwu-realm.json", "/idp-realm.json")
        .withEnv("KC_SPI_EVENTS_LISTENER_REMOVE_USER_ON_LOGOUT_FWU", "IDP")
        .withEnv("KC_SPI_REMOVE_USER_REST_CLIENT_DEFAULT_LICENSE_CONNECT_API_KEY", "sample-api-key")
        .withEnv("KC_SPI_REMOVE_USER_REST_CLIENT_DEFAULT_LICENSE_CONNECT_BASE_URL", "http://mockserver:1080/v1/licences/release");

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

    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user signs out
     * THEN: user is removed from the Keycloak and the license is also released
     */
    @Order(10)
    @Test
    void should_remove_user_and_license()
        throws JsonProcessingException
    {
        try (
                MockServerClient mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())) {
            Expectation releaseLicense = releaseLicenseExpectation(mockServerClient);
            UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
            KeycloakPage kcPage = KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");
            int usersCountBeforeLogout = usersResource.count();

            kcPage.logout();

            int usersCountAfterLogout = usersResource.count();
            assertEquals(usersCountBeforeLogout - 1, usersCountAfterLogout);
            mockServerClient.verify(releaseLicense.getId(), VerificationTimes.once());
        }
    }

    private Expectation releaseLicenseExpectation(MockServerClient clientAndServer)
        throws JsonProcessingException
    {
        RemoveLicenseRequest licenseRequestedRequest = new RemoveLicenseRequest("9c7e5634-5021-4c3e-9bea-53f54c299a0f");
        return clientAndServer
            .when(
                  request().withPath("/v1/licences/release")
                      .withMethod("POST")
                      .withHeader("X-API-Key", "sample-api-key")
                      .withBody(objectMapper.writeValueAsString(licenseRequestedRequest)),
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
        keycloak.start();
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
        keycloak.stop();
        firefoxStandalone.stop();
        mockServer.stop();
    }

    @AfterEach
    void cleanUp()
    {
        driver.quit();
    }
}
