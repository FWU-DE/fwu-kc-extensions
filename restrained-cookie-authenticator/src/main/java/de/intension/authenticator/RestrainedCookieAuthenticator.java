package de.intension.authenticator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.browser.CookieAuthenticator;
import org.keycloak.models.AuthenticatorConfigModel;

/**
 * Cookie authenticator that restrains SSO-cookie shortcutting for a configurable list of identity providers.
 * <p>
 * This Keycloak instance mainly acts as a broker in front of several IdPs. Some of these IdPs require the
 * full authentication round trip on every login instead of being short-circuited via an already existing
 * SSO session. This authenticator wraps the flow context with a {@link RestrainingAuthenticationFlowContext}
 * so that a cookie-based re-authentication is only accepted ({@code success()}) if the current user is
 * neither linked to one of the configured {@code restrainingIdPs} nor carries a {@code pairwiseSub}
 * attribute; otherwise the login is marked as {@code attempted()}, forcing the flow to continue with the
 * full round trip.
 */
public class RestrainedCookieAuthenticator extends CookieAuthenticator
{

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        List<String> restrainingIdPs = Optional.ofNullable(context.getAuthenticatorConfig())
                .map(AuthenticatorConfigModel::getConfig)
                .map(config -> config.get(RestrainedCookieAuthenticatorFactory.CONFIG_RESTRAINING_IDPS))
                .map(value -> Arrays.asList(value.split("##")))
                .orElse(List.of());
        super.authenticate(new RestrainingAuthenticationFlowContext(context, restrainingIdPs));
    }

}
