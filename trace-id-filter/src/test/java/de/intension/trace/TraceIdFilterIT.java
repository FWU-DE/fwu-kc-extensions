package de.intension.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import de.intension.keycloak.IntensionKeycloakContainer;

/**
 * GIVEN: Keycloak with the {@link TraceIdFilter} deployed and a log format containing the trace ID MDC value
 * WHEN: a browser login is started (with trace ID in header, query parameter or none) and continued with wrong credentials
 * THEN: the login error logged in the second request carries the trace ID of the first request, except for the master realm.
 */
@Testcontainers
class TraceIdFilterIT
{

    private static final String  REALM          = "trace";
    private static final String  CLIENT_ID      = "trace-client";
    private static final Pattern FORM_ACTION    = Pattern.compile("action=\"([^\"]+)\"");
    private static final String  LOG_FORMAT     = "%d{HH:mm:ss,SSS} (%t) [%X{trace-id}] %-5p [%c] %s%e%n";

    @Container
    private static final IntensionKeycloakContainer keycloak = new IntensionKeycloakContainer()
        .withProviderClassesFrom("target/classes")
        .withContextPath("/auth")
        .withEnv("KC_LOG_CONSOLE_FORMAT", LOG_FORMAT);

    @BeforeAll
    static void setupRealm()
    {
        var realm = new RealmRepresentation();
        realm.setRealm(REALM);
        realm.setEnabled(true);
        var client = new ClientRepresentation();
        client.setClientId(CLIENT_ID);
        client.setPublicClient(true);
        client.setRedirectUris(List.of("*"));
        realm.setClients(List.of(client));
        keycloak.getKeycloakAdminClient().realms().create(realm);
        keycloak.getKeycloakAdminClient().realm("master").clients().create(client).close();
    }

    @AfterAll
    static void dumpLogs()
        throws IOException
    {
        Files.writeString(Path.of("target/keycloak.log"), keycloak.getLogs());
    }

    @Test
    void should_keep_trace_id_from_query_parameter_during_login_flow()
        throws Exception
    {
        failedLogin(REALM, "user-query", null, "query-123");

        assertLogLine("query-123", "user-query");
    }

    @Test
    void should_keep_trace_id_from_header_during_login_flow()
        throws Exception
    {
        failedLogin(REALM, "user-header", "header-456", null);

        assertLogLine("header-456", "user-header");
    }

    @Test
    void should_keep_generated_trace_id_during_login_flow()
        throws Exception
    {
        failedLogin(REALM, "user-generated", null, null);

        assertLogLine("generated_[A-Za-z]{10}", "user-generated");
    }

    @Test
    void should_not_set_trace_id_for_master_realm()
        throws Exception
    {
        failedLogin("master", "user-master", "master-789", "master-789");

        assertLogLine("", "user-master");
    }

    /**
     * Starts a login (trace ID only sent in the first request) and posts wrong credentials, which
     * makes Keycloak log a LOGIN_ERROR event within the second request.
     */
    private static void failedLogin(String realm, String username, String header, String query)
        throws IOException, InterruptedException
    {
        HttpClient client = HttpClient.newHttpClient();
        String authUrl = keycloak.getAuthServerUrl() + "/realms/" + realm + "/protocol/openid-connect/auth"
                + "?client_id=" + CLIENT_ID
                + "&response_type=code&scope=openid"
                + "&redirect_uri=" + URLEncoder.encode(keycloak.getAuthServerUrl() + "/realms/" + realm + "/cb", StandardCharsets.UTF_8)
                + (query != null ? "&x-trace-id=" + query : "");
        var authRequest = HttpRequest.newBuilder(URI.create(authUrl));
        if (header != null) {
            authRequest.header(TraceId.TRACE_ID_PARAMETER, header);
        }
        HttpResponse<String> loginPage = client.send(authRequest.build(), HttpResponse.BodyHandlers.ofString());
        Matcher matcher = FORM_ACTION.matcher(loginPage.body());
        assertThat(matcher.find()).as("login form in: %s", loginPage.body()).isTrue();
        String formAction = matcher.group(1).replace("&amp;", "&");

        // Keycloak sets "Secure" cookies which java.net.CookieManager does not send back via http
        String cookies = loginPage.headers().allValues("Set-Cookie").stream()
            .map(c -> c.split(";", 2)[0])
            .collect(Collectors.joining("; "));
        String form = "username=" + username + "&password=wrong&credentialId=";
        client.send(HttpRequest.newBuilder(URI.create(formAction))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Cookie", cookies)
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertLogLine(String traceIdPattern, String username)
        throws InterruptedException
    {
        Pattern expected = Pattern.compile("\\[" + traceIdPattern + "] WARN .*LOGIN_ERROR.*username=\"" + username + "\"");
        List<String> lines = List.of();
        for (int attempt = 0; attempt < 20 && lines.isEmpty(); attempt++) {
            Thread.sleep(500);
            lines = keycloak.getLogs().lines().filter(l -> l.contains("username=\"" + username + "\"")).toList();
        }
        assertThat(lines).as("LOGIN_ERROR log line for %s", username).anyMatch(l -> expected.matcher(l).find());
    }
}
