package de.intension.authentication.authenticators.licence;

import de.intension.keycloak.IntensionKeycloakContainer;
import de.intension.testhelper.KeycloakPage;
import de.intension.testhelper.LicenceMockHelper;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static de.intension.authentication.authenticators.licence.LicenceConnectAuthenticatorFactory.*;
import static de.intension.rest.licence.model.LicenseConstants.LICENCE_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

/**
 * Please use the test order in the file to avoid changing configurations again and again
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
public class LicenceConnectAuthenticatorIT {

    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";
    private static final String REALM = "fwu";
    private static final String EXPECTED_LICENCES = "[{\"licenceCode\":\"VHT-9234814-fk68-acbj6-3o9jyfilkq2pqdmxy0j\"},{\"licenceCode\":\"COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015-COR-3rw46a45-345c-4237-a451-4333736ex015\"}]";
    private static final String EXPECTED_LICENCES_BILO = "{\"id\":\"sample-user-id\",\"first_name\":\"Max\",\"last_name\":\"Muster\",\"licenses\":[\"ucs-license-1\",\"ucs-license-2\"],\"context\":{\"additionalProp1\":{\"licenses\":[\"ucs-license-prop-1\",\"ucs-license-prop-2\"],\"classes\":[{\"name\":\"class-1\",\"id\":\"sample-id1\",\"licenses\":[\"uc";
    private static final String PERSISTED_EXPECTED_LICENCES_BILO = "{\"id\":\"sample-user-id\",\"first_name\":\"Max\",\"last_name\":\"Muster\",\"licenses\":[\"ucs-license-1\",\"ucs-license-2\"],\"context\":{\"additionalProp1\":{\"licenses\":[\"ucs-license-prop-1\",\"ucs-license-prop-2\"],\"classes\":[{\"name\":\"class-1\",\"id\":\"sample-id1\",\"licenses\":[\"ucs-class-license-1\",\"ucs-classlicense-2\"]}],\"workgroups\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"school_authority\":\"string\",\"school_identifier\":\"string\",\"school_name\":\"string\",\"roles\":[\"string\"]},\"additionalProp2\":{\"licenses\":[\"string\"],\"classes\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"workgroups\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"school_authority\":\"string\",\"school_identifier\":\"string\",\"school_name\":\"string\",\"roles\":[\"string\"]},\"additionalProp3\":{\"licenses\":[\"string\"],\"classes\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"workgroups\":[{\"name\":\"string\",\"id\":\"string\",\"licenses\":[\"string\"]}],\"school_authority\":\"string\",\"school_identifier\":\"string\",\"school_name\":\"string\",\"roles\":[\"string\"]}}}";

    private static final Network network = Network.newNetwork();
    private static final Capabilities capabilities = new FirefoxOptions();

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("keycloak")
            .withUsername("keycloak")
            .withPassword("test123")
            .withEnv("TZ", "Europe/Berlin")
            .withCommand("postgres", "-c", "timezone=Europe/Berlin");
    ;

    @Container
    private static final MockServerContainer mockServer = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.13.2"))
            .withNetwork(network)
            .withNetworkAliases("mockserver");

    @Container
    private static final IntensionKeycloakContainer keycloak = new IntensionKeycloakContainer()
            .withProviderClassesFrom("target/classes")
            .withProviderLibsFrom(List.of(new File("../target/hmac-mapper.jar")))
            .withContextPath("/auth")
            .withNetwork(network)
            .withNetworkAliases("test")
            .withClasspathResourceMapping("fwu-realm.json", IMPORT_PATH + "fwu-realm.json", BindMode.READ_ONLY)
            .withClasspathResourceMapping("idp-realm.json", IMPORT_PATH + "idp-realm.json", BindMode.READ_ONLY)
            .withRealmImportFiles("/fwu-realm.json", "/idp-realm.json")
            .withEnv("KC_SPI_REST_CLIENT_DEFAULT_LICENCE_CONNECT_BASE_URL", "http://mockserver:1080")
            .withEnv("KC_SPI_REST_CLIENT_DEFAULT_LICENCE_CONNECT_API_KEY", "sample-api-key")
            .withEnv("KC_DB", "postgres")
            .withEnv("KC_DB_URL_HOST", "postgres")
            .withEnv("KC_DB_USERNAME", "keycloak")
            .withEnv("KC_DB_PASSWORD", "test123")
            .withEnv("TZ", "Europe/Berlin")
            .withEnv("JAVA_OPTS", "-Duser.timezone=Europe/Berlin")
            .withEnv("KC_LOG_LEVEL", "INFO,de.intension:debug")
            .dependsOn(postgres, mockServer);

    @Container
    private static final BrowserWebDriverContainer<?> selenium = new BrowserWebDriverContainer<>()
            .withCapabilities(capabilities)
            .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null)
            .withEnv("TZ", "Europe/Berlin")
            .withNetwork(network);

    private static MockServerClient mockServerClient;

    private RemoteWebDriver driver;
    private FluentWait<WebDriver> wait;

    @BeforeAll
    static void setupAll() {
        mockServerClient = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());
        Keycloak admin = keycloak.getKeycloakAdminClient();
        var realm = admin.realm(REALM);
        var upconfig = realm.users().userProfile().getConfiguration();
        upconfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        realm.users().userProfile().update(upconfig);
    }

    @BeforeEach
    void setup() {
        driver = new RemoteWebDriver(selenium.getSeleniumAddress(), capabilities, false);
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(5, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
    }

    /**
     * GIVEN: a user is federated by idp login and client config for license connect is generic LC
     * WHEN: the same user logs in
     * THEN: licence is fetched for the user and added as user attribute
     */
    @Order(10)
    @Test
    void should_add_licence_to_user()
            throws Exception {
        // given
        Expectation requestLicence = LicenceMockHelper.requestLicenceExpectation(mockServerClient);
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");

        // then
        List<UserRepresentation> idpUsers = usersResource.searchByUsername("idpuser", true);
        assertFalse(idpUsers.isEmpty());
        UserRepresentation idpUser = idpUsers.getFirst();
        mockServerClient.verify(requestLicence.getId(), VerificationTimes.once());
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT w.content FROM Licence w");
        resultSet.next();
        String persistedLicence = resultSet.getString(1);
        assertEquals(EXPECTED_LICENCES, persistedLicence);
    }

    /**
     * GIVEN: a user is federated by idp login and client config for license connect is bilo LC
     * WHEN: the same user logs in
     * THEN: licence is fetched for the user and added as user attribute
     */
    @Order(40)
    @Test
    void should_add_licence_to_user_from_bilo()
            throws Exception {
        // given
        Expectation requestLicence = LicenceMockHelper.requestLicenceExpectationBilo(mockServerClient);
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        AuthenticatorConfigRepresentation authConfig = keycloak.getKeycloakAdminClient().realms().realm(REALM).flows()
                .getAuthenticatorConfig("443d2a41-f72a-41fe-af08-a5888ec1c193");
        Map<String, String> config = authConfig.getConfig();
        config.put(SCHOOLIDS_ATTRIBUTE, "prefixedSchools");
        config.put(GENERIC_LICENSE_CLIENTS, "client1");
        config.put(BILO_LICENSE_CLIENTS, "account-console");
        authConfig.setConfig(config);
        keycloak.getKeycloakAdminClient().realms().realm(REALM).flows().updateAuthenticatorConfig("443d2a41-f72a-41fe-af08-a5888ec1c193", authConfig);
        KeycloakPage
                .start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");

        // then
        List<UserRepresentation> idpUsers = usersResource.searchByUsername("idpuser", true);
        assertFalse(idpUsers.isEmpty());
        UserRepresentation idpUser = idpUsers.getFirst();
        mockServerClient.verify(requestLicence.getId(), VerificationTimes.once());
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT w.content FROM Licence w");
        resultSet.next();
        String persistedLicence = resultSet.getString(1);
        assertEquals(PERSISTED_EXPECTED_LICENCES_BILO, persistedLicence);
    }

    /**
     * GIVEN: a user is federated by idp and already has an outdated licence in the database
     * WHEN: the same user logs in
     * THEN: licence is fetched for the user and updated in the database
     */
    @Order(20)
    @Test
    void should_update_licence()
            throws Exception {
        // given
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        statement
                .executeUpdate("INSERT INTO LICENCE (HMAC_ID, CONTENT, CREATED_AT, UPDATED_AT) VALUES ('aece4884-4b58-391f-b83a-ad268906142a', 'Sample Licence Content', LOCALTIMESTAMP, LOCALTIMESTAMP)");
        ResultSet resultSet = statement.executeQuery("SELECT CREATED_AT, UPDATED_AT FROM LICENCE WHERE HMAC_ID = 'aece4884-4b58-391f-b83a-ad268906142a'");
        resultSet.next();
        LocalDateTime createdAt = resultSet.getObject("created_at", LocalDateTime.class);
        LocalDateTime updatedAt = resultSet.getObject("updated_at", LocalDateTime.class);
        assertEquals(updatedAt, createdAt, "UPDATED_AT should be the same as CREATED_AT");

        await().atMost(2, TimeUnit.SECONDS).until(insertIsDone());

        LicenceMockHelper.requestLicenceExpectation(mockServerClient);
        KeycloakPage
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
        createdAt = resultSet.getObject("created_at", LocalDateTime.class);
        updatedAt = resultSet.getObject("updated_at", LocalDateTime.class);

        assertNotEquals(updatedAt, createdAt, "UPDATED_AT should not be the same as CREATED_AT");
        assertTrue(updatedAt.isAfter(createdAt), "UPDATED_AT should be after CREATED_AT");

        // Assert the content is as expected
        String persistedLicence = resultSet.getString(3);
        assertEquals(EXPECTED_LICENCES, persistedLicence, "Licence content does not match");
    }

    private Callable<Boolean> insertIsDone() {
        return () -> {
            Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1 FROM LICENCE WHERE HMAC_ID = 'aece4884-4b58-391f-b83a-ad268906142a'");
            return resultSet.next();
        };
    }

    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user logs in
     * THEN: licence is fetched for the user and error occurs and user attribute is not added
     */
    @Order(30)
    @Test
    void should_not_add_licence_to_user()
            throws Exception {
        // given & when
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage.start(driver, wait)
                .openAccountConsole()
                .idpLogin("idpuser", "test");

        // then
        List<UserRepresentation> idpUsers = usersResource.searchByUsername("idpuser", true);
        assertFalse(idpUsers.isEmpty());
        UserRepresentation idpUser = idpUsers.getFirst();
        List<String> attributes = idpUser.getAttributes().get(LICENCE_ATTRIBUTE);
        assertNull(attributes);
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT w.content FROM Licence w");
        assertFalse(resultSet.next());
    }

    @AfterEach
    void cleanUp()
            throws SQLException {
        Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Statement statement = connection.createStatement();
        statement.executeUpdate("DELETE FROM Licence");
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        UserRepresentation idpUser = usersResource.searchByUsername("idpuser", true).getFirst();
        usersResource.delete(idpUser.getId());
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }
}
