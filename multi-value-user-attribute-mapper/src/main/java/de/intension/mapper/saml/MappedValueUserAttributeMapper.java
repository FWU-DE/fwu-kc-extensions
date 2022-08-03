package de.intension.mapper.saml;

import java.util.*;
import java.util.regex.PatternSyntaxException;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intension.mapper.helper.RegExHelper;

/**
 * Identity provider mapper which imports incoming IdP attributes/claims to user attributes. In
 * addition, incoming values
 * can be translated to other values.
 */
public class MappedValueUserAttributeMapper
        extends UserAttributeMapper
{

    public static final String                        PROVIDER_ID_PREFIX       = "mapped-value-";
    public static final String                        TYPE_AND_CATEGORY_PREFIX = "Mapped Value ";
    public static final String                        VALUE_MAPPINGS           = "attribute.value.mappings";
    public static final String                        FIELD_LABEL              = "User Value Mappings";
    public static final String                        FIELD_HELP_TEXT          = "Key/Value mappings to translate incoming value to other value. Wildcards like \"*\" and \"?\" can be used."
            + " For more flexibility, regular expressions are allowed as well e.g. REGEX_(<expression>).";
    private static final List<ProviderConfigProperty> customConfigProperties   = new ArrayList<>();
    private static final Logger                       logger                   = LoggerFactory.getLogger(MappedValueUserAttributeMapper.class);

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(VALUE_MAPPINGS);
        property.setLabel(FIELD_LABEL);
        property.setHelpText(FIELD_HELP_TEXT);
        property.setType(ProviderConfigProperty.MAP_TYPE);
        customConfigProperties.add(property);
    }

    private final String providerId      = PROVIDER_ID_PREFIX.concat(super.getId());
    private final String typeAndCategory = TYPE_AND_CATEGORY_PREFIX.concat(super.getDisplayCategory());

    /**
     * Set mapped value as user attribute from context.
     */
    public static void setMappedValueAsUserAttribute(UserModel user, IdentityProviderMapperModel mapperModel,
                                                     BrokeredIdentityContext context)
    {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        if (attribute != null) {
            List<String> contextAttrValues = getUserAttribute(context, attribute);
            List<String> userAttribute = user.getAttributes().get(attribute);
            if (userAttribute != null && !CollectionUtil.collectionEquals(contextAttrValues, userAttribute)) {
                user.setAttribute(attribute, contextAttrValues);
            }
        }
    }

    /**
     * Set mapped value as user attribute inside identity context.
     */
    public static void setMappedValueInContext(IdentityProviderMapperModel mapperModel,
                                               BrokeredIdentityContext context)
    {
        String attribute = mapperModel.getConfig().get(USER_ATTRIBUTE);
        if (StringUtil.isNotBlank(attribute)) {
            List<String> attributeValues = getUserAttribute(context, attribute);
            if (!attributeValues.isEmpty()) {
                Set<String> mappedValues = mapValues(mapperModel, attributeValues);
                if (!CollectionUtil.collectionEquals(mappedValues, attributeValues)) {
                    context.setUserAttribute(attribute, new ArrayList<>(mappedValues));
                }
            }
        }
    }

    /**
     * Map incoming value list based on a key/value mapping configuration.
     */
    private static Set<String> mapValues(IdentityProviderMapperModel mapperModel, List<String> attributeValues)
    {
        Set<String> mappedValues = new HashSet<>();
        Map<String, String> configMap = mapperModel.getConfigMap(VALUE_MAPPINGS);
        if (!configMap.isEmpty()) {
            for (String attributeValue : attributeValues) {
                translateAndAddValue(mapperModel, configMap, attributeValue, mappedValues);
            }
        }
        return mappedValues;
    }

    /**
     * Translate single incoming value and add it to the Set of translated values.
     */
    private static void translateAndAddValue(IdentityProviderMapperModel mapperModel, Map<String, String> configMap, String attributeValue,
                                             Set<String> mappedValues)
    {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String key = entry.getKey();
            try {
                if (key.equals(attributeValue)
                        || RegExHelper.isRegularExpression(key) && RegExHelper.matches(RegExHelper.getRegularExpressionFromString(key), attributeValue)
                        || RegExHelper.isWildcardExpression(key) && RegExHelper.matches(RegExHelper.wildcardToJavaRegex(key), attributeValue)) {
                    mappedValues.add(entry.getValue());
                    break;
                }
            } catch (PatternSyntaxException e) {
                logger.warn("IdP Mapper - {} - Invalid regex configured: {}", key, mapperModel.getName());
            }
        }
    }

    private static List<String> getUserAttribute(BrokeredIdentityContext context, String attributeName)
    {
        List<String> userAttribute = (List<String>)context.getContextData().get(Constants.USER_ATTRIBUTES_PREFIX + attributeName);
        if (userAttribute == null) {
            return new ArrayList<>();
        }
        else {
            return userAttribute;
        }
    }

    @Override
    public String getId()
    {
        return providerId;
    }

    @Override
    public String getDisplayCategory()
    {
        return typeAndCategory;
    }

    @Override
    public String getDisplayType()
    {
        return getDisplayCategory();
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        List<ProviderConfigProperty> mergedConfig = new ArrayList<>();
        mergedConfig.addAll(super.getConfigProperties());
        mergedConfig.addAll(customConfigProperties);
        return mergedConfig;
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel,
                                            BrokeredIdentityContext context)
    {
        super.preprocessFederatedIdentity(session, realm, mapperModel, context);
        setMappedValueInContext(mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                                   BrokeredIdentityContext context)
    {
        super.updateBrokeredUser(session, realm, user, mapperModel, context);
        setMappedValueAsUserAttribute(user, mapperModel, context);

    }
}
