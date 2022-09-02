package de.intension.authentication;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator;
import org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticatorFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;

/**
 * Same as {@link IdentityProviderAuthenticator} but with configurable IdP hint
 * parameter name instead of the hardcoded 'kc_idp_hint'.
 */
public class ConfigurableIdpHintParamIdentityProviderAuthenticator
        extends IdentityProviderAuthenticator implements IdpHintParamName {

    private static final Logger LOG = Logger.getLogger(ConfigurableIdpHintParamIdentityProviderAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String idpHintParamName = getIdpHintParamName(context);
        if (context.getUriInfo().getQueryParameters().containsKey(idpHintParamName)) {
            String providerId = context.getUriInfo().getQueryParameters()
                    .getFirst(idpHintParamName);
            if (providerId == null || providerId.equals("")) {
                LOG.tracef("Skipping: IdP hint query parameter is empty");
                context.attempted();
            } else {
                LOG.tracef("Redirecting: %s set to %s", idpHintParamName, providerId);
                redirect(context, providerId);
            }
        } else if (context.getAuthenticatorConfig() != null
                && context.getAuthenticatorConfig().getConfig()
                .containsKey(IdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER)) {
            if (context.getForwardedErrorMessage() != null) {
                LOG.infof(
                        "Should redirect to remote IdP but forwardedError has value '%s', skipping this authenticator...",
                        context.getForwardedErrorMessage());
                context.attempted();

                return;
            }

            String defaultProvider = context.getAuthenticatorConfig().getConfig()
                    .get(IdentityProviderAuthenticatorFactory.DEFAULT_PROVIDER);
            LOG.tracef("Redirecting: default provider set to %s", defaultProvider);
            redirect(context, defaultProvider);
        } else {
            LOG.tracef("No default provider set or %s query parameter provided",
                    idpHintParamName);
            context.attempted();
        }
    }

    private void redirect(AuthenticationFlowContext context, String providerId) {
        Optional<IdentityProviderModel> idp = context.getRealm().getIdentityProvidersStream()
                .filter(IdentityProviderModel::isEnabled)
                .filter(identityProvider -> Objects.equals(providerId, identityProvider.getAlias()))
                .findFirst();
        if (idp.isPresent()) {
            String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(),
                    context.getAuthenticationSession()).getOrGenerateCode();
            String clientId = context.getAuthenticationSession().getClient().getClientId();
            String tabId = context.getAuthenticationSession().getTabId();
            URI location = Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), providerId,
                    context.getRealm().getName(), accessCode, clientId, tabId);
            if (context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY) != null) {
                location = UriBuilder.fromUri(location).queryParam(OAuth2Constants.DISPLAY,
                                context.getAuthenticationSession().getClientNote(OAuth2Constants.DISPLAY))
                        .build();
            }
            Response response = Response.seeOther(location)
                    .build();
            // will forward the request to the IDP with prompt=none if the IDP accepts
            // forwards with prompt=none.
            if ("none".equals(context.getAuthenticationSession().getClientNote(OIDCLoginProtocol.PROMPT_PARAM)) &&
                    Boolean.parseBoolean(idp.get().getConfig().get(ACCEPTS_PROMPT_NONE))) {
                context.getAuthenticationSession().setAuthNote(AuthenticationProcessor.FORWARDED_PASSIVE_LOGIN, "true");
            }
            LOG.debugf("Redirecting to %s", providerId);
            context.forceChallenge(response);
            return;
        }

        LOG.warnf("Provider not found or not enabled for realm %s", providerId);
        context.attempted();
    }
}
