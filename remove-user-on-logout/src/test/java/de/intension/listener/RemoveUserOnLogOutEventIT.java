package de.intension.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.UsersResource;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import dasniko.testcontainers.keycloak.KeycloakContainer;

class RemoveUserOnLogOutEventIT
{

    private final static String        REALM             = "fwu";

    private static Network             network           = Network.newNetwork();

    private static KeycloakContainer   keycloak          = new KeycloakContainer("quay.io/keycloak/keycloak:18.0.2")
        .withProviderClassesFrom("target/classes")
        .withNetwork(network)
        .withNetworkAliases("test")
        .withRealmImportFiles("/fwu-realm.json", "/idp-realm.json")
        .withContextPath("/auth/")
        .withAccessToHost(true);

    private static GenericContainer<?> firefoxStandalone = new GenericContainer<>(DockerImageName.parse("selenium/standalone-firefox:4.3.0-20220706"))
        .withNetwork(network)
        .withNetworkAliases("test")
        .withExposedPorts(4444, 5900)
        .withSharedMemorySize(2000000000l);

    private RemoteWebDriver            driver;
    private FluentWait<WebDriver>      wait;

    /**
     * GIVEN: a user is federated by idp login
     * WHEN: the same user logout
     * THEN: user is removed from the Keycloak
     */
    @Test
    void should_remove_user_on_logout_for_identity_provider_login()
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

    /**
     * GIVEN: a user login with out identity federation
     * WHEN: the same user logout
     * THEN: user is not removed from the Keycloak
     */
    @Test
    void should_not_remove_user_on_logout_for_non_identity_provider_login()
    {
        UsersResource usersResource = keycloak.getKeycloakAdminClient().realms().realm(REALM).users();
        KeycloakPage kcPage = KeycloakPage
            .start(driver, wait)
            .openAccountConsole()
            .normalLogin("misty", "test");
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
    void should_not_remove_user_when_other_event_than_logout()
    {
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

    /**
     * Container for handling interactions with the Keycloak web pages.
     */
    private static class KeycloakPage
    {

        private WebDriver             driver;
        private FluentWait<WebDriver> wait;

        private KeycloakPage(WebDriver driver, FluentWait<WebDriver> wait)
        {
            this.driver = driver;
            this.wait = wait;
        }

        static KeycloakPage start(WebDriver driver, FluentWait<WebDriver> wait)
        {
            return new KeycloakPage(driver, wait);
        }

        public KeycloakPage openAccountConsole()
        {
            driver.get("http://test:8080/auth/realms/fwu/account/");
            return this;
        }

        public KeycloakPage normalLogin(String username, String password)
        {
            return login(username, password);
        }

        public KeycloakPage idpLogin(String username, String password)
        {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("social-keycloak-oidc")));
            driver.findElement(By.id("social-keycloak-oidc")).click();
            return login(username, password);
        }

        private KeycloakPage login(String username, String password)
        {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input#username")));
            driver.findElement(By.cssSelector("input#username")).sendKeys(username);
            driver.findElement(By.cssSelector("input#password")).sendKeys(password);
            driver.findElement(By.cssSelector("input#kc-login")).click();
            return this;
        }

        private KeycloakPage updateLastName(String lastName)
        {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input#lastName")));

            driver.findElement(By.cssSelector("input#lastName")).clear();
            driver.findElement(By.cssSelector("input#lastName")).sendKeys(lastName);
            driver.findElement(By.className("btn-primary")).click();

            return this;
        }

        public KeycloakPage logout()
        {
            String signOutButton = "Sign Out";
            wait.until(ExpectedConditions.elementToBeClickable(By.linkText(signOutButton)));
            driver.findElement(By.linkText(signOutButton)).click();
            return this;
        }

    }

}
