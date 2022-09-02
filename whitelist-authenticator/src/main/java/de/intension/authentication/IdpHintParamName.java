package de.intension.authentication;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;

import javax.ws.rs.core.Response;

public interface IdpHintParamName {

    String IDP_HINT_PARAM_NAME = "idpHintParamName";

    /**
     * Get the IdP hint parameter name from the authenticator config.
     * A missing parameter name is a misconfiguration and results in an internal server error.
     */
    default String getIdpHintParamName(AuthenticationFlowContext context) {
        String idpHintParamName = context.getAuthenticatorConfig().getConfig()
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
