package de.intension.authentication.authenticators.licence;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.intension.testhelper.KeycloakPage;
import de.intension.testhelper.LicenceMockHelper;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockserver.client.MockServerClient;
import org.mockserver.mock.Expectation;
import org.mockserver.verify.VerificationTimes;
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
import java.sql.*;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class LicenceConnectAuthenticatorIT {

    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";
    private static final String REALM = "fwu";
    private static final String EXPECTED_LICENCES = "[\"VHT-9234814-fk68-acbj6-3o9jyfilkq2pqdmxy0j\",\"COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015\"]";

    private static final Network network = Network.newNetwork();
    private static final Capabilities capabilities = new FirefoxOptions();

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

    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user logs in
     * THEN: licence is fetched for the user and added as user attribute
     */
    @Test
    void should_add_licence_to_user() throws Exception {
        // given
        Expectation requestLicence = LicenceMockHelper.requestLicenceExpectation(mockServerClient);
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage kcPage = KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");

        // then
        List<UserRepresentation> idpUsers = usersResource.searchByUsername("idpuser", true);
        assertFalse(idpUsers.isEmpty());
        UserRepresentation idpUser = idpUsers.get(0);
        List<String> attributes = idpUser.getAttributes().get(LicenceConnectAuthenticator.LICENCE_ATTRIBUTE + "1");
        assertFalse(attributes.isEmpty());
        String licenceAttribute = attributes.get(0);
        assertEquals(EXPECTED_LICENCES, licenceAttribute);
        mockServerClient.verify(requestLicence.getId(), VerificationTimes.once());
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT w.content FROM Licence w");
        resultSet.next();
        String persistedLicence = resultSet.getString(1);
        assertEquals(EXPECTED_LICENCES, persistedLicence);
    }

    /**
     * GIVEN: a user is federated by idp and already has an outdated licence in the database
     * WHEN: the same user logs in
     * THEN: licence is fetched for the user and updated in the database
     */
    @Test
    void should_update_licence() throws Exception {
        // given
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        statement.executeUpdate("INSERT INTO LICENCE (HMAC_ID, CONTENT, CREATED_AT, UPDATED_AT) VALUES ('aece4884-4b58-391f-b83a-ad268906142a', 'Sample Licence Content', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        ResultSet resultSet = statement.executeQuery("SELECT CREATED_AT, UPDATED_AT FROM LICENCE WHERE HMAC_ID = 'aece4884-4b58-391f-b83a-ad268906142a'");
        resultSet.next();
        Timestamp updatedAt = resultSet.getTimestamp(1);
        Timestamp createdAt = resultSet.getTimestamp(2);
        assertEquals(updatedAt, createdAt, "UPDATED_AT should be the same as CREATED_AT");

        LicenceMockHelper.requestLicenceExpectation(mockServerClient);
        KeycloakPage kcPage = KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                // when
                .idpLogin("idpuser", "test");

        // then
        resultSet = statement.executeQuery("SELECT COUNT(*) FROM LICENCE");
        resultSet.next();

        // Assert that there is exactly one entry in the table
        int rowCount = resultSet.getInt(1);
        assertEquals(1, rowCount, "Expected exactly one entry in the LICENCE table");

        resultSet = statement.executeQuery("SELECT CREATED_AT, UPDATED_AT, CONTENT FROM LICENCE WHERE HMAC_ID = 'aece4884-4b58-391f-b83a-ad268906142a'");
        resultSet.next();
        // Assert that UPDATED_AT is not the same as CREATED_AT
        createdAt = resultSet.getTimestamp(1);
        updatedAt = resultSet.getTimestamp(2);
        assertNotEquals(updatedAt, createdAt, "UPDATED_AT should not be the same as CREATED_AT");
        assertThat(updatedAt.toLocalDateTime()).isAfter(createdAt.toLocalDateTime());

        // Assert the content is as expected
        String persistedLicence = resultSet.getString(3);
        assertEquals(EXPECTED_LICENCES, persistedLicence, "Licence content does not match");
    }

    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user logs in
     * THEN: licence is fetched for the user and error occurs and user attribute is not added
     */
    @Test
    void should_not_add_licence_to_user() throws Exception {
        // given & when
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage.start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");

        // then
        List<UserRepresentation> idpUsers = usersResource.searchByUsername("idpuser", true);
        assertFalse(idpUsers.isEmpty());
        UserRepresentation idpUser = idpUsers.get(0);
        List<String> attributes = idpUser.getAttributes().get(LicenceConnectAuthenticator.LICENCE_ATTRIBUTE);
        assertNull(attributes);
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT w.content FROM Licence w");
        assertFalse(resultSet.next());
    }

    @AfterEach
    void cleanUp() throws SQLException {
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        statement.executeUpdate("DELETE FROM Licence");
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        UserRepresentation idpUser = usersResource.searchByUsername("idpuser", true).get(0);
        usersResource.delete(idpUser.getId());
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }
}
