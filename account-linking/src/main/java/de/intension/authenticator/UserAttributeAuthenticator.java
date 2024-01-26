package de.intension.authenticator;

import static de.intension.authenticator.UserAttributeAuthenticatorFactory.CONF_ACCOUNT_LINK_ATTRIBUTE;
import static de.intension.authenticator.UserAttributeAuthenticatorFactory.CONF_IDP_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.ErrorPage;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

public class UserAttributeAuthenticator
    implements Authenticator
{

    private static final Logger logger = Logger.getLogger(UserAttributeAuthenticator.class);
    private static final String FORM_FIELD_USERNAME = "username";

    @Override
    public void authenticate(AuthenticationFlowContext context)
    {
        String accountLinkAttrKey = getConfigValue(context, CONF_ACCOUNT_LINK_ATTRIBUTE);
        String idpAlias = getConfigValue(context, CONF_IDP_NAME);
        if (accountLinkAttrKey == null) {
            setMissingConfigurationContext(context, CONF_ACCOUNT_LINK_ATTRIBUTE);
        }
        else if (idpAlias == null) {
            setMissingConfigurationContext(context, CONF_IDP_NAME);
        }
        else {
            context.challenge(getLoginFormResponse(context, idpAlias, accountLinkAttrKey, null));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context)
    {
        String accountLinkAttributeKey = getConfigValue(context, CONF_ACCOUNT_LINK_ATTRIBUTE);
        String idpAlias = getConfigValue(context, CONF_IDP_NAME);
        if (accountLinkAttributeKey == null) {
            setMissingConfigurationContext(context, CONF_ACCOUNT_LINK_ATTRIBUTE);
        }
        else if (idpAlias == null) {
            setMissingConfigurationContext(context, CONF_IDP_NAME);
        }
        else {
            MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
            if (!formData.containsKey("cancel")) {
                String customAttributeValue = formData.getFirst(FORM_FIELD_USERNAME);
                if (customAttributeValue == null || customAttributeValue.isEmpty()) {
                    List<FormMessage> errors = new ArrayList<>();
                    errors.add(new FormMessage(FORM_FIELD_USERNAME, "missingUsernameMessage"));
                    context.challenge(getLoginFormResponse(context, idpAlias, accountLinkAttributeKey, errors));
                    return;
                }
                UserModel user = context.getUser();
                user.setSingleAttribute(accountLinkAttributeKey, customAttributeValue);
            }
            else {
                logger.info("custom idp account linking aborted by user");
            }
            context.success();
        }
    }

    private String getAccountLinkAttrValueFromUser(AuthenticationFlowContext context, String attributeKey)
    {
        String accountLinkingValue = null;
        if (context.getUser() != null) {
            String alValue = context.getUser().getFirstAttribute(attributeKey);
            if (alValue != null && !alValue.isEmpty()) {
                accountLinkingValue = alValue;
            }
        }
        return accountLinkingValue;
    }

    private String getConfigValue(AuthenticationFlowContext context, String key)
    {
        Map<String, String> config = context.getAuthenticatorConfig().getConfig();
        String configValue = config.get(key);
        if (configValue != null && configValue.isBlank()) {
            configValue = null;
        }
        return configValue;
    }

    private void setMissingConfigurationContext(AuthenticationFlowContext context, String property)
    {
        Response response = ErrorPage.error(context.getSession(), context.getAuthenticationSession(),
                                            Response.Status.FORBIDDEN,
                                            "Missing configuration " + property + " for authenticator " + UserAttributeAuthenticatorFactory.PROVIDER_ID);
        context.failureChallenge(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR, response);
    }

    private String getIdpName(AuthenticationFlowContext context, String idpAlias)
    {
        if (idpAlias != null) {
            Optional<IdentityProviderModel> identityProvider = context.getRealm().getIdentityProvidersStream().filter(idp -> idp.getAlias().equals(idpAlias))
                .findFirst();
            if (identityProvider.isPresent() && identityProvider.get().getDisplayName() != null) {
                return identityProvider.get().getDisplayName();
            }
        }
        return idpAlias;
    }

    private Response getLoginFormResponse(AuthenticationFlowContext context, String idpAlias, String accountLinkAttrKey, List<FormMessage> errors)
    {
        LoginFormsProvider forms = context.form();
        forms.setAttribute("idpAlias", getIdpName(context, idpAlias));
        String accountLinkValue = getAccountLinkAttrValueFromUser(context, accountLinkAttrKey);
        if(accountLinkValue != null){
            forms.setAttribute("username_attr", accountLinkValue);
        }
        if (errors != null && !errors.isEmpty()) {
            forms.setErrors(errors);
        }
        return forms.createForm("custom-account-link.ftl");
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
