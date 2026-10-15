package de.intension.trace;

import static de.intension.trace.TraceId.TRACE_ID_KEY;
import static de.intension.trace.TraceId.TRACE_ID_PARAMETER;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.utils.KeycloakSessionUtil;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

/**
 * Determines a trace ID for every request (header {@code x-trace-id}, query parameter
 * {@code x-trace-id} or generated), puts it into the logging MDC under the key {@code trace-id} and
 * keeps it consistent across the steps of an authentication flow by storing it as auth note in the
 * authentication session.
 * <p>
 * Requests to the master realm and to static resources are not processed.
 */
@Provider
public class TraceIdFilter
    implements ContainerRequestFilter, ContainerResponseFilter
{

    static final List<String>   EXCLUDED_PATHS        = List.of("/realms/master", "/resources");
    static final String         TRACE_ID_PROPERTY     = TraceIdFilter.class.getName() + ".traceId";
    private static final String REALMS_PATH           = "/realms/";
    private static final Logger LOG                   = Logger.getLogger(TraceIdFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext)
    {
        String path = normalize(requestContext.getUriInfo().getPath());
        if (isExcluded(path)) {
            return;
        }
        String traceId = getTraceIdFromRequestOrCreate(requestContext);
        AuthenticationSessionModel authSession = findAuthenticationSession(requestContext, path);
        if (authSession != null) {
            String traceIdFromSession = authSession.getAuthNote(TRACE_ID_KEY);
            if (traceIdFromSession == null) {
                authSession.setAuthNote(TRACE_ID_KEY, traceId);
            }
            else {
                traceId = traceIdFromSession;
            }
        }
        requestContext.setProperty(TRACE_ID_PROPERTY, traceId);
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * The authentication session is usually created while processing the very first request of a
     * flow (e.g. the authorization endpoint), i.e. after the request filter ran. Store the trace ID
     * of that request in the new session so that the following steps of the flow reuse it. Runs
     * before Keycloak commits the transaction and closes the session.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
    {
        if (!(requestContext.getProperty(TRACE_ID_PROPERTY) instanceof String traceId)) {
            return;
        }
        try {
            KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
            AuthenticationSessionModel authSession = session == null ? null : session.getContext().getAuthenticationSession();
            if (authSession != null && authSession.getAuthNote(TRACE_ID_KEY) == null && isAlive(session, authSession)) {
                authSession.setAuthNote(TRACE_ID_KEY, traceId);
            }
        } catch (RuntimeException e) {
            LOG.debugf(e, "Could not store trace ID in authentication session");
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    static String getTraceIdFromRequestOrCreate(ContainerRequestContext requestContext)
    {
        String traceId = requestContext.getHeaderString(TRACE_ID_PARAMETER);
        if (traceId == null) {
            traceId = getQueryParameterIgnoreCase(requestContext.getUriInfo().getQueryParameters(), TRACE_ID_PARAMETER);
        }
        if (traceId != null && !TraceId.isValid(traceId)) {
            LOG.debug("Ignoring invalid trace ID from request, generating a new one");
            traceId = null;
        }
        return traceId != null ? traceId : TraceId.generate();
    }

    static boolean isExcluded(String path)
    {
        return EXCLUDED_PATHS.stream().anyMatch(excluded -> path.equals(excluded) || path.startsWith(excluded + "/"));
    }

    /**
     * The realm and the authentication session are not yet set in the Keycloak context when the
     * request filter runs, so they have to be looked up manually: realm from the path, root
     * session from the auth session cookie and the authentication session by the {@code tab_id}
     * query parameter. If there is no {@code tab_id} (e.g. broker endpoint) but the root session
     * only contains a single authentication session, that one is used.
     */
    private static AuthenticationSessionModel findAuthenticationSession(ContainerRequestContext requestContext, String path)
    {
        try {
            KeycloakSession session = KeycloakSessionUtil.getKeycloakSession();
            String realmName = getRealmName(path);
            if (session == null || realmName == null) {
                return null;
            }
            RealmModel realm = session.realms().getRealmByName(realmName);
            if (realm == null) {
                return null;
            }
            if (session.getContext().getRealm() == null) {
                // needed for reading the auth session cookie, would be set by Keycloak later on anyway
                session.getContext().setRealm(realm);
            }
            RootAuthenticationSessionModel rootSession = new AuthenticationSessionManager(session).getCurrentRootAuthenticationSession(realm);
            if (rootSession == null) {
                return null;
            }
            Map<String, AuthenticationSessionModel> authSessions = rootSession.getAuthenticationSessions();
            String tabId = requestContext.getUriInfo().getQueryParameters().getFirst(Constants.TAB_ID);
            if (tabId != null) {
                return authSessions.get(tabId);
            }
            return authSessions.size() == 1 ? authSessions.values().iterator().next() : null;
        } catch (RuntimeException e) {
            LOG.debugf(e, "Could not determine authentication session for trace ID");
            return null;
        }
    }

    /**
     * Prevents re-creating an authentication session which was already removed during the
     * request (e.g. after a successful login).
     */
    private static boolean isAlive(KeycloakSession session, AuthenticationSessionModel authSession)
    {
        RootAuthenticationSessionModel rootSession = session.authenticationSessions()
            .getRootAuthenticationSession(authSession.getRealm(), authSession.getParentSession().getId());
        return rootSession != null && rootSession.getAuthenticationSessions().containsKey(authSession.getTabId());
    }

    static String getRealmName(String path)
    {
        if (!path.startsWith(REALMS_PATH)) {
            return null;
        }
        int end = path.indexOf('/', REALMS_PATH.length());
        String realmName = end < 0 ? path.substring(REALMS_PATH.length()) : path.substring(REALMS_PATH.length(), end);
        return realmName.isEmpty() ? null : realmName;
    }

    private static String getQueryParameterIgnoreCase(MultivaluedMap<String, String> queryParameters, String name)
    {
        if (queryParameters == null) {
            return null;
        }
        return queryParameters.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase(name) && e.getValue() != null && !e.getValue().isEmpty())
            .map(e -> e.getValue().getFirst())
            .findFirst()
            .orElse(null);
    }

    private static String normalize(String path)
    {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
