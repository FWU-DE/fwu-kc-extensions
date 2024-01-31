package de.intension.authenticator;

import java.util.Arrays;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class IdPLinkConditionalAuthenticatorFactory
    implements ConditionalAuthenticatorFactory
{

    public static final String                                      PROVIDER_ID         = "conditional-idp-link";
    public static final String                                      CONF_IDP_NAME       = "idp_name";
    public static final String                                      CONF_NOT            = "not";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public void init(Config.Scope scope)
    {
        //nothing to do
    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory)
    {
        //nothing to do
    }

    @Override
    public void close()
    {
        //nothing to do
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType()
    {
        return "Condition - user refers to IdP";
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
        return "Flow is executed only if the user is linked to the specified identity provider";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        ProviderConfigProperty idpAlias = new ProviderConfigProperty();
        idpAlias.setType(ProviderConfigProperty.STRING_TYPE);
        idpAlias.setName(CONF_IDP_NAME);
        idpAlias.setLabel("Identity Provider alias");
        idpAlias.setHelpText("Alias of the Identity Provider to check");

        ProviderConfigProperty negateOutput = new ProviderConfigProperty();
        negateOutput.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negateOutput.setName(CONF_NOT);
        negateOutput.setLabel("Negate output");
        negateOutput.setHelpText("Apply a not to the check result");

        return Arrays.asList(idpAlias, negateOutput);
    }

    @Override
    public ConditionalAuthenticator getSingleton()
    {
        return IdPLinkConditionalAuthenticator.SINGLETON;
    }

}
