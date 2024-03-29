package de.intension.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.*;
import org.keycloak.admin.client.resource.UsersResource;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import dasniko.testcontainers.keycloak.KeycloakContainer;

class RemoveUserOnLogOutEventIT
{

    private final static String        REALM             = "fwu";

    private static Network             network           = Network.newNetwork();

    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";

    @Container
    private static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:22.0.4")
        .withProviderClassesFrom("target/classes")
        .withContextPath("/auth")
        .withNetwork(network)
        .withNetworkAliases("test")
        .withClasspathResourceMapping("fwu-realm.json",IMPORT_PATH + "fwu-realm.json", BindMode.READ_ONLY)
        .withClasspathResourceMapping("idp-realm.json", IMPORT_PATH + "idp-realm.json", BindMode.READ_ONLY)
        .withRealmImportFiles("/fwu-realm.json","/idp-realm.json")
        .withAccessToHost(true);

    private static GenericContainer<?> firefoxStandalone = new GenericContainer<>(DockerImageName.parse("selenium/standalone-firefox:4.3.0-20220706"))
        .withNetwork(network)
        .withNetworkAliases("test")
        .withExposedPorts(4444, 5900)
        .withSharedMemorySize(2000000000L);

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
                .login("misty", "test");
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
            driver.get("http://test:8080/auth/realms/fwu/account/#/personal-info");
            return this;
        }

        public KeycloakPage idpLogin(String username, String password)
        {
            wait.until(ExpectedConditions.elementToBeClickable(By.id("social-keycloak-oidc")));
            driver.findElement(By.id("social-keycloak-oidc")).click();
            return login(username, password);
        }

        private KeycloakPage login(String username, String password)
        {
            By usernameInput = By.cssSelector("input#username");
            wait.until(ExpectedConditions.elementToBeClickable(By.id("kc-login")));
            wait.until(ExpectedConditions.presenceOfElementLocated(usernameInput));
            driver.findElement(usernameInput).sendKeys(username);
            driver.findElement(By.cssSelector("input#password")).sendKeys(password);
            driver.findElement(By.cssSelector("input#kc-login")).click();
            return this;
        }

        private KeycloakPage updateLastName(String lastName)
        {
            ((RemoteWebDriver) driver).getScreenshotAs(OutputType.FILE);
            wait.until(ExpectedConditions.elementToBeClickable(By.id("save-btn")));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input#last-name")));

            driver.findElement(By.cssSelector("input#last-name")).clear();
            driver.findElement(By.cssSelector("input#last-name")).sendKeys(lastName);
            driver.findElement(By.id("save-btn")).click();

            return this;
        }

        public KeycloakPage logout()
        {
            By signOutButton = By.xpath("//button[text()='Sign out']");
            ((RemoteWebDriver) driver).getScreenshotAs(OutputType.FILE);
            wait.until(ExpectedConditions.elementToBeClickable(signOutButton));
            driver.findElement(signOutButton).click();
            return this;
        }

    }

}
