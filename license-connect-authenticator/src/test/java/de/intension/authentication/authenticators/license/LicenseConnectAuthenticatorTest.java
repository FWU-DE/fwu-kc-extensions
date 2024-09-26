package de.intension.authentication.authenticators.license;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.HttpStatusCode.OK_200;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
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
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.intension.authentication.authenticators.rest.model.LicenseRequestedRequest;
import de.intension.authentication.helpers.KeycloakPage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LicenseConnectAuthenticatorTest
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
        .withClasspathResourceMapping("fwu-realm-license.json", IMPORT_PATH + "fwu-realm-license.json", BindMode.READ_ONLY)
        .withClasspathResourceMapping("idp-realm-license.json", IMPORT_PATH + "idp-realm-license.json", BindMode.READ_ONLY)
        .withRealmImportFiles("/fwu-realm-license.json", "/idp-realm-license.json")
        .withEnv("KC_SPI_AUTHENTICATOR_LICENSE_CONNECT_AUTHENTICATOR_LICENSE_URL", "http://mockserver:1080/v1/licences/request")
        .withEnv("KC_SPI_AUTHENTICATOR_LICENSE_CONNECT_AUTHENTICATOR_LICENSE_API_KEY", "sample-api-key");

    private static final GenericContainer<?> firefoxStandalone = new GenericContainer<>(DockerImageName.parse("selenium/standalone-firefox:4.3.0-20220706"))
        .withNetwork(network)
        .withNetworkAliases("test")
        .withExposedPorts(4444, 5900)
        .withSharedMemorySize(2000000000L);

    private static final GenericContainer<?> mockServer        = new GenericContainer<>(DockerImageName.parse("mockserver/mockserver:5.13.2"))
        .withNetwork(network)
        .withNetworkAliases("mockserver")
        .withExposedPorts(1080, 1090);

    private RemoteWebDriver                  driver;
    private FluentWait<WebDriver>            wait;

    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user logs in
     * THEN: license is fetched for the user and added as user attribute
     */
    @Order(10)
    @Test
    void should_add_license_to_user()
        throws JsonProcessingException
    {
        try (
                MockServerClient mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getFirstMappedPort())) {
            Expectation requestLicense = requestLicenseExpectation(mockServerClient);
            UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
            KeycloakPage kcPage = KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");
            List<UserRepresentation> idpUsers = usersResource.searchByUsername("idpuser", true);
            assertFalse(idpUsers.isEmpty());
            UserRepresentation idpUser = idpUsers.get(0);
            List<String> attributes = idpUser.getAttributes().get(LicenseConnectAuthenticator.LICENSE_ATTRIBUTE + "1");
            assertFalse(attributes.isEmpty());
            String licenseAttribute = attributes.get(0);
            assertEquals("[{\"license_code\":\"VHT-9234814-fk68-acbj6-3o9jyfilkq2pqdmxy0j\"},{\"license_code\":\"COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015\"}]",
                         licenseAttribute);
            mockServerClient.verify(requestLicense.getId(), VerificationTimes.once());
            kcPage.logout();
        }
    }

    private Expectation requestLicenseExpectation(MockServerClient clientAndServer)
        throws JsonProcessingException
    {
        LicenseRequestedRequest licenseRequestedRequest = new LicenseRequestedRequest("9c7e5634-5021-4c3e-9bea-53f54c299a0f", "account-console",
                "DE-SN-Schullogin.0815", "de-DE");
        return clientAndServer
            .when(
                  request().withPath("/v1/licences/request")
                      .withMethod("POST")
                      .withHeader("X-API-Key", "sample-api-key")
                      .withBody(objectMapper.writeValueAsString(licenseRequestedRequest)),
                  Times.exactly(1))
            .respond(
                     response()
                         .withStatusCode(OK_200.code())
                         .withReasonPhrase(OK_200.reasonPhrase())
                         .withBody("{\n    \"hasLicences\": true,\n    \"licences\": [\n      {\n        \"license_code\": \"VHT-9234814-fk68-acbj6-3o9jyfilkq2pqdmxy0j\"\n      },\n      {\n        \"license_code\": \"COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015\"\n      }\n    ]\n  }")
                         .withHeaders(
                                      header(CONTENT_TYPE.toString(), MediaType.JSON_UTF_8.getType())))[0];
    }

    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user logs in
     * THEN: license is fetched for the user and error occurs and user attribute is not added
     */
    @Order(20)
    @Test
    void should_not_add_license_to_user()
    {
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage.start(driver, wait)
            .openAccountConsole()
            .idpLogin("idpuser", "test");
        List<UserRepresentation> idpUsers = usersResource.searchByUsername("idpuser", true);
        assertFalse(idpUsers.isEmpty());
        UserRepresentation idpUser = idpUsers.get(0);
        List<String> attributes = idpUser.getAttributes().get(LicenseConnectAuthenticator.LICENSE_ATTRIBUTE);
        assertNull(attributes);
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
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        UserRepresentation idpUser = usersResource.searchByUsername("idpuser", true).get(0);
        usersResource.delete(idpUser.getId());
    }
}
