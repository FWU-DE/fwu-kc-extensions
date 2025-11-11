package de.intension.mapper.helper;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.PatternSyntaxException;

import static org.keycloak.broker.saml.mappers.UserAttributeMapper.USER_ATTRIBUTE;

public class AttributeMapperHelper
{
    public static final String  VALUE_MAPPINGS           = "attribute.value.mappings";
    public static final String  PROVIDER_ID_PREFIX       = "mapped-value-";
    public static final String  TYPE_AND_CATEGORY_PREFIX = "Mapped Value ";
    public static final String  FIELD_LABEL              = "User Value Mappings";
    public static final String  FIELD_HELP_TEXT          = "Key/Value mappings to translate incoming value to other value. Wildcards like \"*\" and \"?\" can be used."
            + " For more flexibility, regular expressions are allowed as well e.g. REGEX_(<expression>).";
    private static final Logger logger                   = LoggerFactory.getLogger(AttributeMapperHelper.class);

    private AttributeMapperHelper(){}

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
        Map<String, List<String>> configMap = mapperModel.getConfigMap(VALUE_MAPPINGS);
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
    private static void translateAndAddValue(IdentityProviderMapperModel mapperModel, Map<String, List<String>> configMap, String attributeValue,
                                             Set<String> mappedValues)
    {
        for (Map.Entry<String, List<String>> entry : configMap.entrySet()) {
            String key = entry.getKey();
            if (key != null) {
                try {
                    if (key.equalsIgnoreCase(attributeValue)
                            || RegExHelper.isRegularExpression(key) && RegExHelper.matches(RegExHelper.getRegularExpressionFromString(key), attributeValue)
                            || RegExHelper.isWildcardExpression(key) && RegExHelper.matches(RegExHelper.wildcardToJavaRegex(key), attributeValue)) {
                        mappedValues.add(entry.getValue().getFirst());
                        break;
                    }
                } catch (PatternSyntaxException e) {
                    logger.warn("IdP Mapper - {} - Invalid regex configured: {}", key, mapperModel.getName());
                }
            }
        }
    }

    /**
     * Get user attribute from brokered identity context.
     */
    @SuppressWarnings("unchecked")
    private static List<String> getUserAttribute(BrokeredIdentityContext context, String attributeName)
    {
        List<String> userAttribute = (List<String>)context.getContextData().get(Constants.USER_ATTRIBUTES_PREFIX + attributeName);
        return Objects.requireNonNullElseGet(userAttribute, ArrayList::new);
    }
}
