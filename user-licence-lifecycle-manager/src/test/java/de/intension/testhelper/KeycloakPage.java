package de.intension.testhelper;

import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Container for handling interactions with the Keycloak web pages.
 */
public class KeycloakPage {

    private final WebDriver driver;
    private final FluentWait<WebDriver> wait;

    private KeycloakPage(WebDriver driver, FluentWait<WebDriver> wait) {
        this.driver = driver;
        this.wait = wait;
    }

    public static KeycloakPage start(WebDriver driver, FluentWait<WebDriver> wait) {
        return new KeycloakPage(driver, wait);
    }

    public KeycloakPage openAccountConsole() {
        driver.get("http://test:8080/auth/realms/fwu/account/#/personal-info");
        return this;
    }

    public KeycloakPage idpLogin(String username, String password) {
        wait.until(ExpectedConditions.elementToBeClickable(By.id("social-keycloak-oidc")));
        driver.findElement(By.id("social-keycloak-oidc")).click();
        return login(username, password);
    }

    public KeycloakPage login(String username, String password) {
        By usernameInput = By.cssSelector("input#username");
        wait.until(ExpectedConditions.elementToBeClickable(usernameInput));
        driver.findElement(usernameInput).sendKeys(username);
        driver.findElement(By.cssSelector("input#password")).sendKeys(password);
        driver.findElement(By.cssSelector("#kc-login")).click();
        return this;
    }

    public KeycloakPage logout() {
        // click to open dropdown
        By userMenuToggle = By.cssSelector("button[data-testid='options-toggle']");
        wait.until(ExpectedConditions.elementToBeClickable(userMenuToggle));
        driver.findElement(userMenuToggle).click();

        // 2. Wait for and click the "Sign out" item
        By signOutButton = By.xpath("//button[.//span[text()='Sign out']]");
        wait.until(ExpectedConditions.visibilityOfElementLocated(signOutButton));
        wait.until(ExpectedConditions.elementToBeClickable(signOutButton));
        driver.findElement(signOutButton).click();

        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            Files.copy(screenshot.toPath(), Paths.get("logout_failure.png"));
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return this;
    }

    public KeycloakPage updateLastName(String lastName) {
        ((RemoteWebDriver) driver).getScreenshotAs(OutputType.FILE);
        wait.until(ExpectedConditions.elementToBeClickable(By.id("save-btn")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("input#lastName")));

        driver.findElement(By.cssSelector("input#lastName")).clear();
        driver.findElement(By.cssSelector("input#lastName")).sendKeys(lastName);
        driver.findElement(By.id("save-btn")).click();

        return this;
    }

}
