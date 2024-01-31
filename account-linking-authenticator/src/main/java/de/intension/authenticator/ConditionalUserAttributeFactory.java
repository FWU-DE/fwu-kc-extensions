package de.intension.authenticator;

import java.util.Arrays;
import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalAuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

public class ConditionalUserAttributeFactory
    implements ConditionalAuthenticatorFactory
{

    public static final String                                      PROVIDER_ID         = "conditional-user-attribute-key";

    public static final String                                      CONF_ATTRIBUTE_NAME = "attribute_name";
    public static final String                                      CONF_NOT            = "not";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public void init(Config.Scope config)
    {
        // no-op
    }

    @Override
    public void postInit(KeycloakSessionFactory factory)
    {
        // no-op
    }

    @Override
    public void close()
    {
        // no-op
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType()
    {
        return "Condition - user attribute key";
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
        return "Flow is executed only if the user attribute exists";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        ProviderConfigProperty attributeName = new ProviderConfigProperty();
        attributeName.setType(ProviderConfigProperty.STRING_TYPE);
        attributeName.setName(CONF_ATTRIBUTE_NAME);
        attributeName.setLabel("Attribute name");
        attributeName.setHelpText("Name of the attribute to check");

        ProviderConfigProperty negateOutput = new ProviderConfigProperty();
        negateOutput.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negateOutput.setName(CONF_NOT);
        negateOutput.setLabel("Negate output");
        negateOutput.setHelpText("Apply a not to the check result");

        return Arrays.asList(attributeName, negateOutput);
    }

    @Override
    public ConditionalAuthenticator getSingleton()
    {
        return ConditionalUserAttribute.SINGLETON;
    }

}
