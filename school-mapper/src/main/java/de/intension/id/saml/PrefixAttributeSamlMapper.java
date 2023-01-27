package de.intension.id.saml;

import static de.intension.id.PrefixAttributeConstants.LOWER_CASE;
import static de.intension.id.PrefixAttributeConstants.PREFIX;
import static org.keycloak.broker.saml.mappers.UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME;
import static org.keycloak.broker.saml.mappers.UserAttributeMapper.ATTRIBUTE_NAME;
import static org.keycloak.broker.saml.mappers.UserAttributeMapper.ATTRIBUTE_NAME_FORMAT;
import static org.keycloak.broker.saml.mappers.UserAttributeMapper.NAME_FORMATS;
import static org.keycloak.broker.saml.mappers.UserAttributeMapper.USER_ATTRIBUTE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.StringUtil;

import de.intension.id.PrefixAttributeService;

/**
 * Identity provider mapper to map SAML attributes to user attributes.
 */
public class PrefixAttributeSamlMapper extends AbstractIdentityProviderMapper
{

    public static final String  PROVIDER_ID = "prefixed-attr-saml-idp-mapper";

    private static final String EMAIL       = "email";
    private static final String FIRST_NAME  = "firstName";
    private static final String LAST_NAME   = "lastName";

    private static final Logger LOG         = Logger.getLogger(PrefixAttributeSamlMapper.class);

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode)
    {
        // supports any sync mode
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        List<ProviderConfigProperty> configProperties = new ArrayList<>();
        var property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME);
        property.setLabel("Attribute Name");
        property.setHelpText("Name of attribute to search for in assertion.  You can leave this blank and specify a friendly name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_FRIENDLY_NAME);
        property.setLabel("Friendly Name");
        property.setHelpText("Friendly name of attribute to search for in assertion.  You can leave this blank and specify a name instead.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE_NAME_FORMAT);
        property.setLabel("Name Format");
        property.setHelpText("Name format of attribute to specify in the RequestedAttribute element. Default to basic format.");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(NAME_FORMATS);
        property.setDefaultValue(JBossSAMLURIConstants.ATTRIBUTE_FORMAT_BASIC.name());
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(USER_ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store saml attribute.  Use email, lastName, and firstName to map to those predefined user properties.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(PREFIX);
        property.setLabel("Prefix");
        property.setHelpText("Prefix to add to the attribute.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(LOWER_CASE);
        property.setLabel("To lowercase");
        property.setHelpText("Transform the attribute value to lowercase.");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        configProperties.add(property);

        return configProperties;
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders()
    {
        return UserAttributeMapper.COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory()
    {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType()
    {
        return "Prefixed Attribute Mapper";
    }

    @Override
    public String getHelpText()
    {
        return "This mapper will prefix a SAML value and store it in a user attribute.";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                              BrokeredIdentityContext context)
    {
        prefix(user, mapperModel, context);
    }

    private void prefix(UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context)
    {
        var config = mapperModel.getConfig();
        String prefix = config.get(PREFIX);
        boolean toLowerCase = Boolean.parseBoolean(config.getOrDefault(LOWER_CASE, Boolean.FALSE.toString()));
        String attribute = config.get(USER_ATTRIBUTE);
        if (StringUtil.isNullOrEmpty(attribute)) {
            return;
        }
        String attributeName = mapperModel.getConfig().get(ATTRIBUTE_NAME);
        String attributeFriendlyName = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);

        List<String> attributeValuesInContext = findAttributeValuesInContext(context, attributeName, attributeFriendlyName);
        if (!attributeValuesInContext.isEmpty()) {
            PrefixAttributeService prefixer = new PrefixAttributeService(prefix, toLowerCase);
            var prefixedValues = prefixer.prefix(attributeValuesInContext);

            if (attribute.equalsIgnoreCase(EMAIL)) {
                setIfNotEmpty(user::setEmail, prefixedValues);
            }
            else if (attribute.equalsIgnoreCase(FIRST_NAME)) {
                setIfNotEmpty(user::setFirstName, prefixedValues);
            }
            else if (attribute.equalsIgnoreCase(LAST_NAME)) {
                setIfNotEmpty(user::setLastName, prefixedValues);
            }
            else {
                user.setAttribute(attribute, prefixedValues);
            }
        }
    }

    /**
     * @see UserAttributeMapper#findAttributeValuesInContext
     */
    private List<String> findAttributeValuesInContext(BrokeredIdentityContext context, String attributeName, String attributeFriendlyName)
    {
        AssertionType assertion = (AssertionType)context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

        var statements = assertion.getAttributeStatements();
        LOG.tracef("Found %d statements", statements.size());
        for (var statement : statements) {
            var attributes = statement.getAttributes();
            for (var attribute : attributes) {
                var attributeType = attribute.getAttribute();
                if (checkAttributeName(attributeType, attributeName, attributeFriendlyName)) {
                    LOG.debug("Found attribute to map: " + attributeType.toString());
                    return attributeType.getAttributeValue().stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .filter(this::notBlank)
                        .collect(Collectors.toList());
                }
            }
        }
        LOG.debug("Found no attribute to map for name: " + attributeName);
        return List.of();
    }

    private boolean checkAttributeName(AttributeType attribute, String attributeName, String attributeFriendlyName)
    {
        if (attributeName != null) {
            return Objects.equals(attribute.getName(), attributeName);
        }
        return Objects.equals(attribute.getFriendlyName(), attributeFriendlyName);
    }

    private boolean notBlank(String value)
    {
        return value != null && !value.isBlank();
    }

    /**
     * @see UserAttributeMapper#setIfNotEmpty
     */
    private void setIfNotEmpty(Consumer<String> consumer, List<String> values)
    {
        if (values != null && !values.isEmpty()) {
            consumer.accept(values.get(0));
        }
    }
}
