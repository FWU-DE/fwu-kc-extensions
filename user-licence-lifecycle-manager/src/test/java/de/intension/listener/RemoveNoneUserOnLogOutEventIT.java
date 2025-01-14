package de.intension.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.intension.rest.model.LicenceRequest;
import de.intension.testhelper.KeycloakPage;
import org.junit.jupiter.api.*;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.keycloak.admin.client.resource.UsersResource;
import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.Times;
import org.mockserver.model.MediaType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

class RemoveNoneUserOnLogOutEventIT {

    private static final String REALM = "fwu";

    private static final Network network = createTestNetwork();

    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
            .withEnv("KC_SPI_EVENTS_LISTENER_REMOVE_USER_ON_LOGOUT_LICENCE_API_KEY", "sample-api-key")
            .withAccessToHost(true);

    private RemoteWebDriver driver;
    private FluentWait<WebDriver> wait;
    private static MockServerClient mockServerClient;


    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user logout
     * THEN: user is removed from the Keycloak
     */
    @Test
    void should_not_remove_user_on_logout_when_realm_not_configured() {
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage kcPage = KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");
        int usersCountBeforeLogout = usersResource.count();

        kcPage.logout();

        int usersCountAfterLogout = usersResource.count();
        assertEquals(usersCountBeforeLogout, usersCountAfterLogout);
    }

    @BeforeAll
    static void configureMockserverClient() {
        keycloak.start();
        mockServerClient = new MockServerClient("localhost", 1080);
    }

    @BeforeEach
    void setupSelenium()
            throws MalformedURLException {
        FirefoxOptions fOptions = new FirefoxOptions();
        driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), fOptions);
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(5, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
    }

    @BeforeEach
    void setupMockServer() throws JsonProcessingException {
        requestLicenceExpectation();
    }

    @AfterAll
    static void stopContainers() {
        keycloak.stop();
    }

    @AfterEach
    void cleanUp() {
        driver.quit();
        mockServerClient.reset();
    }

    private static Network createTestNetwork() {
        return new Network() {
            @Override
            public String getId() {
                return "resources_fwu_test";
            }

            @Override
            public void close() {
                // No-op
            }

            @Override
            public Statement apply(Statement var1, Description var2) {
                return null;
            }
        };
    }

    private void requestLicenceExpectation()
            throws JsonProcessingException {
        LicenceRequest licenceRequestedRequest = new LicenceRequest("9c7e5634-5021-4c3e-9bea-53f54c299a0f", "account-console",
                "DE-SN-Schullogin.0815", "de-DE");
        mockServerClient
                .when(
                        request().withPath("/v1/licences/request")
                                .withMethod("POST")
                                .withHeader("X-API-Key", "sample-api-key")
                                .withBody(objectMapper.writeValueAsString(licenceRequestedRequest)),
                        Times.exactly(1))
                .respond(
                        response()
                                .withStatusCode(OK_200.code())
                                .withReasonPhrase(OK_200.reasonPhrase())
                                .withBody("{\n    \"hasLicences\": true,\n    \"licences\": [\n      {\n        \"licence_code\": \"VHT-9234814-fk68-acbj6-3o9jyfilkq2pqdmxy0j\"\n      },\n      {\n        \"licence_code\": \"COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015\"\n      }\n    ]\n  }")
                                .withHeaders(
                                        header(CONTENT_TYPE, MediaType.JSON_UTF_8.getType())));
    }
}
