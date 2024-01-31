package de.intension.authenticator;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class UserAttributeAuthenticatorFactory
    implements AuthenticatorFactory
{

    public static final String PROVIDER_ID                 = "user-attribute-authenticator";
    public static final String CONF_ACCOUNT_LINK_ATTRIBUTE = "account_link_attribute";
    public static final String CONF_IDP_NAME = "idp_name";

    @Override
    public String getDisplayType()
    {
        return "Account linking target input Form";
    }

    @Override
    public String getReferenceCategory()
    {
        return "Input field to User attribute";
    }

    @Override
    public boolean isConfigurable()
    {
        return true;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices()
    {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed()
    {
        return false;
    }

    @Override
    public String getHelpText()
    {
        return "Ask via input field for a value and stores it as a user attribute";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        ProviderConfigProperty accountLinkAttribute = new ProviderConfigProperty();
        accountLinkAttribute.setType(ProviderConfigProperty.STRING_TYPE);
        accountLinkAttribute.setName(CONF_ACCOUNT_LINK_ATTRIBUTE);
        accountLinkAttribute.setLabel("Account linking attribute");
        accountLinkAttribute.setHelpText("Attribute key for the account link");

        ProviderConfigProperty idpAlias = new ProviderConfigProperty();
        idpAlias.setType(ProviderConfigProperty.STRING_TYPE);
        idpAlias.setName(CONF_IDP_NAME);
        idpAlias.setLabel("Identity Provider alias");
        idpAlias.setHelpText("Alias is used to get the display name for the IdP so that it can be displayed as the target IdP in the description");

        return List.of(accountLinkAttribute, idpAlias);
    }

    @Override
    public Authenticator create(KeycloakSession keycloakSession)
    {
        return new UserAttributeAuthenticator();
    }

    @Override
    public void init(Config.Scope scope)
    {
        //do nothing
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory)
    {
        //do nothing
    }

    @Override
    public void close()
    {
        //do nothing
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }
}
