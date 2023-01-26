package de.intension.id.saml;

import static de.intension.id.oidc.PrefixAttributeOidcMapper.LOWER_CASE;
import static de.intension.id.oidc.PrefixAttributeOidcMapper.PREFIX;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.saml.common.util.StringUtil;

import de.intension.id.PrefixAttributeService;

/**
 * Identity provider mapper to map SAML attributes to user attributes.
 */
public class PrefixAttributeSamlMapper extends UserAttributeMapper
{

    public static final String  PROVIDER_ID = "prefixed-attr-saml-idp-mapper";

    private static final String EMAIL       = "email";
    private static final String FIRST_NAME  = "firstName";
    private static final String LAST_NAME   = "lastName";

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode)
    {
        // supports any sync mode
        return true;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        var configProperties = super.getConfigProperties();
        var prefixProperty = configProperties.stream()
            .filter(p -> PREFIX.equals(p.getName()))
            .findFirst()
            .orElse(null);
        if (prefixProperty == null) {
            var property = new ProviderConfigProperty();
            property.setName(PREFIX);
            property.setLabel("Prefix");
            property.setHelpText("Prefix to add to the claim.");
            property.setType(ProviderConfigProperty.STRING_TYPE);
            configProperties.add(property);
        }
        var lowercaseProperty = configProperties.stream()
            .filter(p -> LOWER_CASE.equals(p.getName()))
            .findFirst()
            .orElse(null);
        if (lowercaseProperty == null) {
            var property = new ProviderConfigProperty();
            property.setName(LOWER_CASE);
            property.setLabel("To lowercase");
            property.setHelpText("Transform the attribute value to lowercase.");
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue(false);
            configProperties.add(property);
        }
        return configProperties;
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
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
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context)
    {
        prefix(mapperModel, context);
    }

    private void prefix(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context)
    {
        var config = mapperModel.getConfig();
        String prefix = config.get(PREFIX);
        boolean toLowerCase = Boolean.parseBoolean(config.getOrDefault(LOWER_CASE, Boolean.FALSE.toString()));
        String attribute = config.get(USER_ATTRIBUTE);
        if (StringUtil.isNullOrEmpty(attribute)) {
            return;
        }
        String attributeName = getAttributeNameFromMapperModel(mapperModel);

        List<String> attributeValuesInContext = findAttributeValuesInContext(attributeName, context);
        if (!attributeValuesInContext.isEmpty()) {
            PrefixAttributeService prefixer = new PrefixAttributeService(prefix, toLowerCase);
            var prefixedValues = prefixer.prefix(attributeValuesInContext);

            if (attribute.equalsIgnoreCase(EMAIL)) {
                setIfNotEmpty(context::setEmail, prefixedValues);
            }
            else if (attribute.equalsIgnoreCase(FIRST_NAME)) {
                setIfNotEmpty(context::setFirstName, prefixedValues);
            }
            else if (attribute.equalsIgnoreCase(LAST_NAME)) {
                setIfNotEmpty(context::setLastName, prefixedValues);
            }
            else {
                context.setUserAttribute(attribute, prefixedValues);
            }
        }
    }

    /**
     * @see UserAttributeMapper#findAttributeValuesInContext
     */
    private List<String> findAttributeValuesInContext(String attributeName, BrokeredIdentityContext context)
    {
        AssertionType assertion = (AssertionType)context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);

        return assertion.getAttributeStatements().stream()
            .flatMap(statement -> statement.getAttributes().stream())
            .filter(elementWith(attributeName))
            .flatMap(attributeType -> attributeType.getAttribute().getAttributeValue().stream())
            .filter(Objects::nonNull)
            .filter(this::listNotEmpty)
            .map(Object::toString)
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private boolean listNotEmpty(Object object)
    {
        if (object instanceof List) {
            var list = (List<Object>)object;
            return !list.isEmpty();
        }
        return true;
    }

    /**
     * @see UserAttributeMapper#elementWith
     */
    private Predicate<AttributeStatementType.ASTChoiceType> elementWith(String attributeName)
    {
        return (attributeType) -> {
            AttributeType attribute = attributeType.getAttribute();
            return Objects.equals(attribute.getName(), attributeName) || Objects.equals(attribute.getFriendlyName(), attributeName);
        };
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

    /**
     * @see UserAttributeMapper#getAttributeNameFromMapperModel
     */
    private String getAttributeNameFromMapperModel(IdentityProviderMapperModel mapperModel)
    {
        String attributeName = mapperModel.getConfig().get(ATTRIBUTE_NAME);
        if (attributeName == null) {
            attributeName = mapperModel.getConfig().get(ATTRIBUTE_FRIENDLY_NAME);
        }
        return attributeName;
    }
}
