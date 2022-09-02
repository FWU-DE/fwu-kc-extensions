package de.intension.authentication;

import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.constants.AdapterConstants;

public interface IdpHintParamName
{

    String IDP_HINT_PARAM_NAME = "idpHintParamName";

    /**
     * Get the IdP hint parameter name from the authenticator config.
     * A missing parameter name is a misconfiguration and results in an internal server error.
     */
    default String getIdpHintParamName(AuthenticationFlowContext context)
    {
        var authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig == null) {
            return AdapterConstants.KC_IDP_HINT;
        }
        String idpHintParamName = authenticatorConfig.getConfig()
            .get(IDP_HINT_PARAM_NAME);
        if (idpHintParamName == null) {
            // throw new AuthenticationFlowException("Authenticator config is missing IdP
            // hint parameter",
            // AuthenticationFlowError.INTERNAL_ERROR);
            Response challenge = context.form()
                .setError("Authenticator config is missing IdP hint parameter")
                .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR, challenge);
        }
        return idpHintParamName;
    }
}
