package de.intension.authentication;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.constants.AdapterConstants;

public interface IdpHintParamName
{

    String IDP_HINT_PARAM_NAME = "idpHintParamName";

    /**
     * Get the IdP hint parameter name from the authenticator config.
     * Will default to {@link AdapterConstants#KC_IDP_HINT} if no value was found.
     */
    default String getIdpHintParamName(AuthenticationFlowContext context)
    {
        var authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig == null) {
            return AdapterConstants.KC_IDP_HINT;
        }
        return authenticatorConfig.getConfig()
            .getOrDefault(IDP_HINT_PARAM_NAME, AdapterConstants.KC_IDP_HINT);
    }
}
