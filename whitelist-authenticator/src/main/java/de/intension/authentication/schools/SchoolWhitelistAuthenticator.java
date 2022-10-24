package de.intension.authentication.schools;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPage;
import org.keycloak.utils.StringUtil;

import de.intension.authentication.IdpHintParamName;
import de.intension.authentication.dto.SchoolWhitelistEntry;

/**
 * Check users school id against service provider white list.
 */
public class SchoolWhitelistAuthenticator
    implements Authenticator, IdpHintParamName
{

    private static final Logger logger = Logger.getLogger(SchoolWhitelistAuthenticator.class);

    private final Object lock = new Object();

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        List<SchoolWhitelistEntry> entries = getWhiteListFromCache(context);
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        List<String> schoolIds = getSchoolIdsFromUser(context);
        if (isPermittedServiceRequest(entries, clientId, schoolIds)) {
            context.success();
        }
        else {
            logger.infof("Service Provider with clientId=%s not configured for schools=%s for User-Id=%s", clientId, Arrays.toString(schoolIds.toArray()),
                         context.getUser().getId());
            context.failure(AuthenticationFlowError.IDENTITY_PROVIDER_DISABLED, createErrorPage(context));
        }
    }

    protected Response createErrorPage(AuthenticationFlowContext context){
        return ErrorPage.error(context.getSession(), context.getAuthenticationSession(),
                               Response.Status.FORBIDDEN, "idpNotConfigured");
    }

    /**
     * Get whitelist entries from cache.
     */
    private List<SchoolWhitelistEntry> getWhiteListFromCache(AuthenticationFlowContext context)
    {
        WhiteListCache cache = WhiteListCache.getInstance();
        CacheStatus cacheStatus;
        synchronized(lock){
            //wait until cache is initialized for the first time
            //first User LOGIN will initialize the cache after server restart
            cacheStatus = cache.getState(getCacheIntervalFromConfig(context));
        }
        if (cacheStatus == CacheStatus.OUTDATED) {
            //refresh cache in the background and do not wait for it
            logger.info("trigger cache update asynchronous");
            triggerAsyncCacheUpdate(context);
        }
        else if (cacheStatus == CacheStatus.NOT_INITIALIZED) {
            //first call, trigger and wait
            logger.info("trigger cache update synchronous");
            synchronized(lock){
                triggerCacheUpdate(context);
            }
            logger.info("cache updated synchronous");
        }
        return cache.getAll();
    }

    /**
     * Get school ids from user attribute.
     */
    private List<String> getSchoolIdsFromUser(AuthenticationFlowContext context)
    {
        List<String> schoolIds = null;
        String schoolsAttributeName = getConfigParam(context, SchoolWhitelistAuthenticatorFactory.USER_ATTRIBUTE_PARAM);
        if (StringUtil.isNotBlank(schoolsAttributeName)) {
            UserModel user = context.getUser();
            if(user != null){
                schoolIds = user.getAttributes().get(schoolsAttributeName);
            }
        }
        else {
            logger.errorf("User attribute must not be blank for provider %s", SchoolWhitelistAuthenticatorFactory.PROVIDER_ID);
        }
        if (schoolIds == null) {
            schoolIds = new ArrayList<>();
        }
        return schoolIds;
    }

    /**
     * Get whitelist URI from @{@link AuthenticationFlowContext}
     */
    private URI getURIFromConfig(AuthenticationFlowContext context)
    {
        String whitelistURI = getConfigParam(context, SchoolWhitelistAuthenticatorFactory.WHITELIST_URI_PARAM);
        URI uri = null;
        if (StringUtil.isBlank(whitelistURI)) {
            logger.errorf("Whitelist URI must not be blank for provider %s", SchoolWhitelistAuthenticatorFactory.PROVIDER_ID);
        }
        else {
            try {
                uri = new URI(whitelistURI);
            } catch (URISyntaxException e) {
                logger.errorf("Invalid uri '%s' configured for provider %s", whitelistURI, SchoolWhitelistAuthenticatorFactory.PROVIDER_ID);
            }
        }
        return uri;
    }

    /**
     * Get cache refresh interval from @{@link AuthenticationFlowContext}
     */
    private int getCacheIntervalFromConfig(AuthenticationFlowContext context)
    {
        int intervalInMinutes = 5; //default
        String interval = getConfigParam(context, SchoolWhitelistAuthenticatorFactory.CACHE_REFRESH_PARAM);
        if (StringUtil.isNotBlank(interval)) {
            try {
                intervalInMinutes = Integer.parseInt(interval);
            } catch (NumberFormatException e) {
                logger.errorf("Invalid interval format %s", interval);
            }
        }
        else {
            logger.errorf("Interval must not be empty for provider %s", SchoolWhitelistAuthenticatorFactory.PROVIDER_ID);
        }
        return intervalInMinutes;
    }

    /**
     * Get config param from @{@link AuthenticationFlowContext}
     */
    private String getConfigParam(AuthenticationFlowContext context, String parameterName)
    {
        String value = null;
        AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig != null) {
            Map<String, String> config = authenticatorConfig.getConfig();
            value = config.get(parameterName);
        }
        return value;
    }

    /**
     * Triggers an asynchronous cache update
     */
    private void triggerAsyncCacheUpdate(AuthenticationFlowContext context)
    {
        URI uri = getURIFromConfig(context);
        if (uri != null) {
            ConfigTask task = new ConfigTask(uri);
            Thread thread = new Thread(task);
            thread.start();
        }
    }

    /**
     * Triggers a synchronous cache update (cache initializing only)
     */
    private void triggerCacheUpdate(AuthenticationFlowContext context)
    {
        URI uri = getURIFromConfig(context);
        if (uri != null) {
            ConfigTask task = new ConfigTask(uri);
            task.run();
        }
    }

    /**
     * Checks, whether the combination of Service Provider ID (clientID) and School ID is part of the
     * whitelist.
     */
    private boolean isPermittedServiceRequest(List<SchoolWhitelistEntry> entries, String clientId, List<String> schoolIds)
    {
        boolean permitted = false;
        List<String> permittedSchools = getPermittedSchoolsByClientId(entries, clientId);
        if (!schoolIds.isEmpty() && !permittedSchools.isEmpty()) {
            for (String userSchoolId : schoolIds) {
                if (permittedSchools.contains(userSchoolId)) {
                    permitted = true;
                    break;
                }
            }
        }
        return permitted;
    }

    /**
     * Get permitted school IDs for a given Service Provider (clientId)
     */
    private List<String> getPermittedSchoolsByClientId(List<SchoolWhitelistEntry> entries, String clientId)
    {
        if (!entries.isEmpty()) {
            for (SchoolWhitelistEntry entry : entries) {
                if (entry.getSpAlias().equals(clientId)) {
                    return entry.getListOfSchools();
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public void action(AuthenticationFlowContext authenticationFlowContext)
    {
        //do nothing
    }

    @Override
    public boolean requiresUser()
    {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel)
    {
        return false;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel)
    {
        //do nothing
    }

    @Override
    public void close()
    {
        //do nothing
    }
}
