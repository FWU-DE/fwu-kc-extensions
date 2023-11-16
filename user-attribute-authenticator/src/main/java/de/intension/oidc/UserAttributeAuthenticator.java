package de.intension.oidc;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorPage;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class UserAttributeAuthenticator
    implements Authenticator
{

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        if (getAccountLinkProperty(context) != null) {
            context.success();
        }
        else {
            LoginFormsProvider forms = context.form();
            context.challenge(forms.createLoginUsername());
        }
    }

    @Override
    public void action(AuthenticationFlowContext context)
    {
        if (getAccountLinkProperty(context) == null) {
            MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
            String customAttributeValue = formData.getFirst("username");

            if (customAttributeValue == null || customAttributeValue.isEmpty()) {
                Response response = ErrorPage.error(context.getSession(), context.getAuthenticationSession(),
                                                    Response.Status.FORBIDDEN, "Missing custom attribute");
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, response);
                return;
            }

            UserModel user = context.getUser();
            user.setSingleAttribute("account_linking_key", customAttributeValue);
        }
        context.success();
    }

    private String getAccountLinkProperty(AuthenticationFlowContext context)
    {
        String accountLinkingValue = null;
        if (context.getUser() != null) {
            String alValue = context.getUser().getFirstAttribute("account_linking_key");
            if (alValue != null && !alValue.isEmpty()) {
                accountLinkingValue = alValue;
            }
        }
        return accountLinkingValue;
    }

    @Override
    public boolean requiresUser()
    {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel)
    {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession keycloakSession, RealmModel realmModel, UserModel userModel)
    {
        //not needed
    }

    @Override
    public void close()
    {
        //not needed
    }
}
