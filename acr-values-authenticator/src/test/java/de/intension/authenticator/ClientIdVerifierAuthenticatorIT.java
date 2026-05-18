package de.intension.authenticator;

import static de.intension.authenticator.ClientIdVerifierAuthenticatorFactory.PROVIDER_ID;
import static de.intension.authenticator.ClientIdVerifierAuthenticatorFactory.USER_ATTRIBUTE_NAME_CONFIG;
import static de.intension.authenticator.ClientIdVerifierAuthenticatorFactory.USER_ATTRIBUTE_NAME_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
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

import de.intension.keycloak.IntensionKeycloakContainer;
import jakarta.ws.rs.core.Response;

/**
 * Integration test for {@link ClientIdVerifierAuthenticator}.
 * <p>
 * Setup: The fwu realm is configured with a browser flow that uses
 * {@link ClientIdIdpValuesForwarderAuth} to store the originating client ID in the
 * authentication session. A post-broker-login flow is created via the admin API that runs
 * {@link ClientIdVerifierAuthenticator}, which checks that the user attribute
 * {@code vidis_client_id} matches the stored session note.
 * <p>
 * The IdP user ({@code idpuser}) is created in the {@code idp} realm and is pre-linked to the
 * fwu realm via a federated identity. The {@code vidis_client_id} attribute on the fwu-side user
 * is manipulated via the admin API to simulate different scenarios without needing a real
 * protocol mapper.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Testcontainers
class ClientIdVerifierAuthenticatorIT {

    private static final Network network = Network.newNetwork();
    private static final String IMPORT_PATH = "/opt/keycloak/data/import/";
    private static final Capabilities capabilities = new FirefoxOptions();

    private static final String FWU_REALM = "fwu";
    private static final String IDP_REALM = "idp";
    private static final String IDP_ALIAS = "keycloak-oidc";
    private static final String IDP_USER = "idpuser";
    private static final String IDP_PASSWORD = "test";
    /** Client that opens the account console – this is what the verifier checks against. */
    private static final String ORIGIN_CLIENT = "account-console";
    private static final String VERIFIER_FLOW_ALIAS = "client-id-verifier-flow";
    private static final String VERIFIER_CONFIG_ALIAS = "clientIdVerifierConfig";

    @Container
    private static final IntensionKeycloakContainer keycloak = new IntensionKeycloakContainer()
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

    // -------------------------------------------------------------------------
    // One-time setup
    // -------------------------------------------------------------------------

    @BeforeAll
    static void setupRealm() {
        Keycloak admin = keycloak.getKeycloakAdminClient();

        // 1. Allow unmanaged attributes in fwu realm so vidis_client_id can be stored
        var fwuUsers = admin.realm(FWU_REALM).users();
        UPConfig upConfig = fwuUsers.userProfile().getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ENABLED);
        fwuUsers.userProfile().update(upConfig);

        // 2. Create idpuser in the IdP realm via admin API so the password is set
        //    correctly regardless of Keycloak version (plain-text import in realm JSON
        //    is unreliable for password credentials).
        var idpUsersResource = admin.realm(IDP_REALM).users();
        UserRepresentation idpUser = new UserRepresentation();
        idpUser.setUsername(IDP_USER);
        idpUser.setEnabled(true);
        idpUser.setEmail("idpuser@example.com");
        idpUser.setEmailVerified(true);
        idpUser.setFirstName("Idp");
        idpUser.setLastName("User");
        String idpUserId;
        try (Response r = idpUsersResource.create(idpUser)) {
            assertEquals(201, r.getStatus(), "Failed to create idpuser in idp realm: " + r.readEntity(String.class));
            idpUserId = r.getLocation().getPath().replaceAll(".*/", "");
        }
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(IDP_PASSWORD);
        credential.setTemporary(false);
        idpUsersResource.get(idpUserId).resetPassword(credential);

        // 3. Create post-broker-login flow with client-id-verifier authenticator in fwu realm
        var flows = admin.realm(FWU_REALM).flows();

        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setAlias(VERIFIER_FLOW_ALIAS);
        flow.setDescription("Post broker login flow that verifies the echoed client ID");
        flow.setProviderId("basic-flow");
        flow.setTopLevel(true);
        flow.setBuiltIn(false);
        try (Response r = flows.createFlow(flow)) {
            assertEquals(201, r.getStatus(), "Failed to create verifier flow: " + r.readEntity(String.class));
        }

        // Add the verifier authenticator execution to the flow
        Map<String, Object> executionData = Map.of(
                "provider", PROVIDER_ID
        );
        flows.addExecution(VERIFIER_FLOW_ALIAS, executionData);

        // Set execution to REQUIRED
        List<AuthenticationExecutionInfoRepresentation> executions = flows.getExecutions(VERIFIER_FLOW_ALIAS);
        assertFalse(executions.isEmpty(), "No executions found in verifier flow");
        AuthenticationExecutionInfoRepresentation execution = executions.get(0);
        execution.setRequirement("REQUIRED");
        flows.updateExecutions(VERIFIER_FLOW_ALIAS, execution);

        // Create authenticator config for the execution (user attribute name)
        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias(VERIFIER_CONFIG_ALIAS);
        config.setConfig(Map.of(USER_ATTRIBUTE_NAME_CONFIG, USER_ATTRIBUTE_NAME_DEFAULT));
        try (Response r = flows.newExecutionConfig(execution.getId(), config)) {
            assertEquals(201, r.getStatus(), "Failed to create verifier authenticator config: " + r.readEntity(String.class));
        }

        // 4. Update the keycloak-oidc IdP: set post-broker-login flow and disable profile
        //    update prompt so no "Update Account Information" dialog interrupts the tests.
        var idpResource = admin.realm(FWU_REALM).identityProviders().get(IDP_ALIAS);
        IdentityProviderRepresentation idp = idpResource.toRepresentation();
        idp.setPostBrokerLoginFlowAlias(VERIFIER_FLOW_ALIAS);
        idp.setUpdateProfileFirstLoginMode("off");
        // Use a first-broker-login flow that creates the user automatically without
        // prompting "Account already exists" – the existing "vidis first broker login" flow
        // uses idp-create-user-if-unique which is sufficient for our pre-linked user.
        idp.setFirstBrokerLoginFlowAlias("vidis first broker login");
        idpResource.update(idp);

        // 5. Pre-create a federated user in the fwu realm linked to the IdP so we can
        //    manipulate the vidis_client_id attribute between test runs without going
        //    through first-broker-login each time.
        //    Email and firstName are set so the "missing" profile check never triggers.
        UserRepresentation fwuUser = new UserRepresentation();
        fwuUser.setUsername(IDP_USER);
        fwuUser.setEnabled(true);
        fwuUser.setEmail("idpuser@example.com");
        fwuUser.setEmailVerified(true);
        fwuUser.setFirstName("Idp");
        fwuUser.setLastName("User");
        fwuUser.setAttributes(Map.of(USER_ATTRIBUTE_NAME_DEFAULT, List.of(ORIGIN_CLIENT)));
        try (Response r = fwuUsers.create(fwuUser)) {
            assertEquals(201, r.getStatus(), "Failed to create linked user in fwu realm: " + r.readEntity(String.class));
        }

        // Link federated identity to keycloak-oidc.
        // The federated identity userId must be the actual UUID of the user in the IdP realm,
        // otherwise Keycloak cannot match the incoming token subject to the pre-linked user
        // and falls through to the "Account already exists" handling.
        String fwuUserId = fwuUsers.searchByUsername(IDP_USER, true).get(0).getId();
        FederatedIdentityRepresentation fedId = new FederatedIdentityRepresentation();
        fedId.setIdentityProvider(IDP_ALIAS);
        fedId.setUserId(idpUserId);
        fedId.setUserName(IDP_USER);
        try (Response r = fwuUsers.get(fwuUserId).addFederatedIdentity(IDP_ALIAS, fedId)) {
            assertEquals(204, r.getStatus(), "Failed to link federated identity: " + r.readEntity(String.class));
        }
    }

    // -------------------------------------------------------------------------
    // Per-test setup / teardown
    // -------------------------------------------------------------------------

    @BeforeEach
    void setup() {
        driver = new RemoteWebDriver(selenium.getSeleniumAddress(), capabilities, false);
        wait = new FluentWait<>(driver);
        wait.withTimeout(Duration.of(15, ChronoUnit.SECONDS));
        wait.pollingEvery(Duration.of(250, ChronoUnit.MILLIS));
    }

    @AfterEach
    void tearDown() {
        driver.quit();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /** Sets the vidis_client_id attribute on the pre-linked fwu user via admin API. */
    private void setVidisClientId(String value) {
        var fwuUsers = keycloak.getKeycloakAdminClient().realm(FWU_REALM).users();
        String userId = fwuUsers.searchByUsername(IDP_USER, true).get(0).getId();
        UserRepresentation user = fwuUsers.get(userId).toRepresentation();
        if (value == null) {
            user.getAttributes().remove(USER_ATTRIBUTE_NAME_DEFAULT);
        } else {
            user.setAttributes(Map.of(USER_ATTRIBUTE_NAME_DEFAULT, List.of(value)));
        }
        fwuUsers.get(userId).update(user);
    }

    private void openAccountConsoleAndLoginViaIdp() {
        driver.get("http://test:8080/auth/realms/fwu/account/#/personal-info");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("social-" + IDP_ALIAS)));
        driver.findElement(By.id("social-" + IDP_ALIAS)).click();
        // Wait for IdP login form to be fully interactive
        By usernameInput = By.cssSelector("input#username");
        wait.until(ExpectedConditions.elementToBeClickable(usernameInput));
        driver.findElement(usernameInput).sendKeys(IDP_USER);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input#password")));
        driver.findElement(By.cssSelector("input#password")).sendKeys(IDP_PASSWORD);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#kc-login")));
        driver.findElement(By.cssSelector("#kc-login")).click();
    }

    private void assertLoginSucceeded() {
        wait.until(ExpectedConditions.urlContains("/account"));
        assertFalse(driver.getCurrentUrl().contains("error"), "Expected successful login but got error page");
    }

    private void assertLoginDenied() {
        wait.until(ExpectedConditions.or(
                ExpectedConditions.urlContains("error"),
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(".kc-feedback-text, .alert-error, #kc-error-message"))
        ));
        String pageSource = driver.getPageSource();
        assertTrue(
                driver.getCurrentUrl().contains("error") || pageSource.contains("error") || pageSource.contains("denied"),
                "Expected an error page but got: " + driver.getCurrentUrl()
        );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * GIVEN: the session note IS set (forwarder ran) but the user has no vidis_client_id attribute
     *        (simulates the IdP not echoing back the client ID claim)
     * WHEN:  the user logs in via IDP
     * THEN:  verification fails because the expected echo is missing
     */
    @Test
    @Order(10)
    void should_deny_when_session_note_present_but_vidis_client_id_attribute_absent() {
        setVidisClientId(null);

        openAccountConsoleAndLoginViaIdp();

        assertLoginDenied();
    }

    /**
     * GIVEN: the user attribute vidis_client_id matches the originating client ID (account-console)
     * WHEN:  the user logs in via IDP
     * THEN:  the verifier confirms the match and login succeeds
     */
    @Test
    @Order(20)
    void should_succeed_when_vidis_client_id_matches_origin_client() {
        setVidisClientId(ORIGIN_CLIENT);

        openAccountConsoleAndLoginViaIdp();

        assertLoginSucceeded();
    }

    /**
     * GIVEN: the user attribute vidis_client_id contains a different client ID than the one that
     *        originated the login request
     * WHEN:  the user logs in via IDP
     * THEN:  the verifier detects the mismatch and authentication fails with an error page
     */
    @Test
    @Order(30)
    void should_deny_when_vidis_client_id_does_not_match_origin_client() {
        setVidisClientId("wrong-client");

        openAccountConsoleAndLoginViaIdp();

        assertLoginDenied();
    }
}
