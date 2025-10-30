package de.intension.authenticator;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class IdpValuesForwarderAuthenticatorTest {

    private static final Network network = Network.newNetwork();
    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";
    private static final Capabilities capabilities = new FirefoxOptions();

    @Container
    private static final KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.4.2")
            .withProviderClassesFrom("target/classes")
            .withContextPath("/auth")
            .withNetwork(network)
            .withNetworkAliases("test")
            .withClasspathResourceMapping("fwu-realm.json", IMPORT_PATH + "fwu-realm.json", BindMode.READ_ONLY)
            .withClasspathResourceMapping("idp-realm.json", IMPORT_PATH + "idp-realm.json", BindMode.READ_ONLY)
            .withClasspathResourceMapping("idp2-realm.json", IMPORT_PATH + "idp2-realm.json", BindMode.READ_ONLY)
            .withRealmImportFiles("/fwu-realm.json", "/idp-realm.json", "/idp2-realm.json");

    @Container
    private static final BrowserWebDriverContainer<?> selenium = new BrowserWebDriverContainer<>()
            .withCapabilities(capabilities)
            .withRecordingMode(BrowserWebDriverContainer.VncRecordingMode.SKIP, null)
            .withNetwork(network);

    private RemoteWebDriver driver;
    private FluentWait<WebDriver> wait;

    @BeforeEach
    void setup() {
        driver = new RemoteWebDriver(selenium.getSeleniumAddress(), capabilities);
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(5, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    @Test
    @Order(10)
    void should_add_additional_params_to_idp_request() {
        openAccountConsole();
        idpLoginPage("social-keycloak-oidc");
        String currentUrl = driver.getCurrentUrl();
        assertTrue(currentUrl.contains("audience=securedapp"));
        assertTrue(currentUrl.contains("acr_values=securedapp"));
    }

    @Test
    @Order(20)
    void should_not_add_additional_params_to_idp_request() {
        openAccountConsole();
        idpLoginPage("social-keycloak-oidc2");
        String currentUrl = driver.getCurrentUrl();
        assertFalse(currentUrl.contains("audience=securedapp"));
    }

    public void openAccountConsole() {
        driver.get("http://test:8080/auth/realms/fwu/account/#/personal-info");
    }

    public void idpLoginPage(String id) {
        wait.until(ExpectedConditions.elementToBeClickable(By.id(id)));
        driver.findElement(By.id(id)).click();
    }
}
