package de.intension.authentication;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WhitelistAuthenticatorIT {

    private HttpServer stub;
    private int stubPort;

    @Container
    private KeycloakContainer keycloak;

    private Playwright playwright;
    private Browser browser;

    @BeforeAll
    void setup() throws Exception {
        // Start stub server (host network)
        stub = HttpServer.create(new InetSocketAddress(0), 0);
        stubPort = stub.getAddress().getPort();
        // Expose host port to container
        org.testcontainers.Testcontainers.exposeHostPorts(stubPort);

        // Handlers
        // token endpoint
        stub.createContext("/auth/realms/test/protocol/openid-connect/token", exchange -> {
            byte[] resp = "{\"access_token\":\"12345\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
        });
        // idp assignments
        stub.createContext("/service-provider", new IdpAssignmentsHandler());
        stub.start();

        // Start Keycloak with our provider and import realm
        keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.4.4")
                .withProviderClassesFrom("target/classes")
                .withContextPath("/auth")
                .withRealmImportFiles("whitelist_whitelist_realm.json")
                // Wire factory init properties to stub server on host
                .withEnv("KC_SPI_AUTHENTICATOR_WHITELIST_AUTHENTICATOR_KC_AUTH_URL", "http://host.testcontainers.internal:" + stubPort + "/auth")
                .withEnv("KC_SPI_AUTHENTICATOR_WHITELIST_AUTHENTICATOR_REST_URL", "http://host.testcontainers.internal:" + stubPort + "/service-provider/%s/idp-assignments");
        keycloak.start();

        // Browser
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    void tearDown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        if (keycloak != null) keycloak.stop();
        if (stub != null) stub.stop(0);
    }

    @Test
    @DisplayName("configured + facebook hint -> allowed (no error page)")
    void should_allow_when_client_configured_and_idp_allowed() {
        Page page = browser.newPage();
        page.navigate(buildAuthUrl("configured", "vidis_idp_hint", "facebook"));
        // Expect to be on login form (no forbidden error page)
        assertTrue(hasLoginForm(page), "Expected Keycloak login form");
        assertFalse(containsForbidden(page), "Did not expect forbidden error page");
        page.close();
    }

    @Test
    @DisplayName("notConfigured + facebook hint -> forbidden error")
    void should_block_when_client_not_configured() {
        Page page = browser.newPage();
        page.navigate(buildAuthUrl("notConfigured", "vidis_idp_hint", "facebook"));
        assertTrue(containsForbidden(page), "Expected forbidden error page");
        page.close();
    }

    @Test
    @DisplayName("configuredNoMatch + facebook hint -> forbidden error")
    void should_block_when_idp_not_in_list() {
        Page page = browser.newPage();
        page.navigate(buildAuthUrl("configuredNoMatch", "vidis_idp_hint", "facebook"));
        assertTrue(containsForbidden(page), "Expected forbidden error page");
        page.close();
    }

    @Test
    @DisplayName("configuredGoogle + facebook hint -> forbidden error (only google allowed)")
    void should_block_when_only_google_allowed() {
        Page page = browser.newPage();
        page.navigate(buildAuthUrl("configuredGoogle", "vidis_idp_hint", "facebook"));
        assertTrue(containsForbidden(page), "Expected forbidden error page");
        page.close();
    }

    @Test
    @DisplayName("configuredGoogle + missing hint -> allowed (no error)")
    void should_allow_when_hint_missing() {
        Page page = browser.newPage();
        page.navigate(buildAuthUrl("configuredGoogle", null, null));
        assertTrue(hasLoginForm(page), "Expected Keycloak login form");
        assertFalse(containsForbidden(page), "Did not expect forbidden error page");
        page.close();
    }

    @Test
    @DisplayName("configuredGoogle + google hint -> allowed (no error)")
    void should_allow_when_google_hint() {
        Page page = browser.newPage();
        page.navigate(buildAuthUrl("configuredGoogle", "vidis_idp_hint", "google"));
        assertTrue(hasLoginForm(page), "Expected Keycloak login form");
        assertFalse(containsForbidden(page), "Did not expect forbidden error page");
        page.close();
    }

    private boolean containsForbidden(Page page) {
        // Keycloak error page shows an element with id 'kc-error-message'
        return page.locator("#kc-error-message").count() > 0;
    }

    private boolean hasLoginForm(Page page) {
        return page.locator("form#kc-form-login").count() > 0;
    }

    private String buildAuthUrl(String clientId, String paramName, String paramValue) {
        String base = keycloak.getAuthServerUrl();
        String redirect = keycloak.getAuthServerUrl() + "/realms/whitelist/account/";
        String ru = URLEncoder.encode(redirect, StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        sb.append(base).append("/realms/whitelist/protocol/openid-connect/auth")
          .append("?client_id=").append(clientId)
          .append("&redirect_uri=").append(ru)
          .append("&response_type=code")
          .append("&scope=openid");
        if (paramName != null && paramValue != null) {
            sb.append("&").append(paramName).append("=").append(URLEncoder.encode(paramValue, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static class IdpAssignmentsHandler implements HttpHandler {
        private final Pattern pattern = Pattern.compile("^/service-provider/([^/]+)/idp-assignments$");
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            Matcher m = pattern.matcher(uri.getPath());
            if (!m.matches()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String clientId = m.group(1);
            String body;
            int status;
            switch (clientId) {
                case "configured":
                    body = "[\"facebook\",\"google\"]";
                    status = 200;
                    break;
                case "configuredGoogle":
                    body = "[\"google\"]";
                    status = 200;
                    break;
                case "configuredNoMatch":
                    body = "[\"github\",\"microsoft\"]";
                    status = 200;
                    break;
                case "notConfigured":
                    body = "";
                    status = 404;
                    break;
                default:
                    body = "";
                    status = 404;
            }
            if (status == 200) {
                byte[] resp = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, resp.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }
}
