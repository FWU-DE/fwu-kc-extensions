package de.intension.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import de.intension.testhelper.KeycloakPage;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.resource.UsersResource;
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

class RemoveAllTypesOnLogOutEventIT
{

    private static final String              REALM             = "fwu";

    private static final Network             network           = Network.newNetwork();

    private static final String              IMPORT_PATH       = "/opt/keycloak/data/import/";

    @Container
    private static final KeycloakContainer   keycloak          = new KeycloakContainer("quay.io/keycloak/keycloak:22.0.4")
        .withProviderClassesFrom("target/classes")
        .withContextPath("/auth")
        .withNetwork(network)
        .withNetworkAliases("test")
        .withClasspathResourceMapping("fwu-realm.json", IMPORT_PATH + "fwu-realm.json", BindMode.READ_ONLY)
        .withClasspathResourceMapping("idp-realm.json", IMPORT_PATH + "idp-realm.json", BindMode.READ_ONLY)
        .withRealmImportFiles("/fwu-realm.json", "/idp-realm.json")
        .withEnv("KC_SPI_EVENTS_LISTENER_REMOVE_USER_ON_LOGOUT_FWU", "ALL")
        .withAccessToHost(true);

    private static final GenericContainer<?> firefoxStandalone = new GenericContainer<>(DockerImageName.parse("selenium/standalone-firefox:4.3.0-20220706"))
        .withNetwork(network)
        .withNetworkAliases("test")
        .withExposedPorts(4444, 5900)
        .withSharedMemorySize(2000000000L);

    private RemoteWebDriver                  driver;
    private FluentWait<WebDriver>            wait;

    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user logout
     * THEN: user is removed from the Keycloak
     */
    @Test
    void should_remove_user_on_logout_for_local_login()
    {
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage kcPage = KeycloakPage
            .start(driver, wait)
            .openAccountConsole()
            .login("misty", "test");

        int usersCountBeforeLogout = usersResource.count();
        kcPage.logout();

        int usersCountAfterLogout = usersResource.count();
        assertEquals(usersCountBeforeLogout -1, usersCountAfterLogout);
    }

    @Test
    void should_remove_user_on_logout_for_Idp_login()
    {
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage kcPage = KeycloakPage
            .start(driver, wait)
            .openAccountConsole()
            .idpLogin("idpuser", "test");

        int usersCountBeforeLogout = usersResource.count();
        kcPage.logout();

        int usersCountAfterLogout = usersResource.count();
        assertEquals(usersCountBeforeLogout - 1, usersCountAfterLogout);
    }

    @BeforeAll
    static void startContainers()
    {
        keycloak.start();
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
        keycloak.stop();
        firefoxStandalone.stop();
    }

    @AfterEach
    void cleanUp()
    {
        driver.quit();
    }

}
