package de.intension.authentication;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import de.intension.keycloak.IntensionKeycloakContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.BindMode;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConfigurableIdpHintParamIdentityProviderAuthenticatorIT {

    private static final String IMPORT_DIR = "/opt/keycloak/data/import/";

    @Container
    private static final IntensionKeycloakContainer keycloak = new IntensionKeycloakContainer()
            .withProviderClassesFrom("target/classes")
            .withContextPath("/auth")
            .withRealmImportFiles("whitelist_realm.json");

    private Playwright playwright;
    private Browser browser;

    @BeforeAll
    void setupBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    void tearDownBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @Order(10)
    void should_redirect_to_idp_when_hint_param_set() {
        Page page = browser.newPage();
        String authUrl = buildAuthUrl("vidis_idp_hint", "facebook");
        page.navigate(authUrl);
        // the authenticator should redirect to the external IDP authorization endpoint
        String url = page.url();
        assertThat(url).contains("facebook.com/oauth/authorize");
        assertTrue(url.contains("facebook.com/oauth/authorize"), () -> "Expected redirect to facebook authorization URL, got: " + url);
        page.close();
    }

    @Test
    @Order(20)
    void should_not_redirect_when_hint_param_unknown() {
        Page page = browser.newPage();
        String authUrl = buildAuthUrl("vidis_idp_hint", "unknown-idp");
        page.navigate(authUrl);
        String url = page.url();
        // should NOT go to broker, will remain on login/authenticate flow
        assertFalse(url.contains("/broker/"), () -> "Did not expect broker redirect for unknown idp, got: " + url);
        page.close();
    }

    private String buildAuthUrl(String paramName, String paramValue) {
        String base = keycloak.getAuthServerUrl(); // ends with /auth
        String redirect = keycloak.getAuthServerUrl() + "/realms/whitelist/account/";
        String ru = URLEncoder.encode(redirect, StandardCharsets.UTF_8);
        return base + "/realms/whitelist/protocol/openid-connect/auth" +
                "?client_id=account" +
                "&redirect_uri=" + ru +
                "&response_type=code" +
                "&scope=openid" +
                "&" + paramName + "=" + URLEncoder.encode(paramValue, StandardCharsets.UTF_8);
    }

    private static String resolveFromProjectRoot(String relative) {
        // module dir is .../whitelist-authenticator; project root is parent directory
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        Path root = moduleDir.getParent();
        return root.resolve(relative).toAbsolutePath().toString();
    }
}
