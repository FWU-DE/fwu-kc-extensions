package de.intension.mapper.saml;

import static org.mockito.Mockito.*;

import java.util.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class MappedValueUserAttributeMapperTest
{

    private static final String ROLE_ATTR_KEY   = "role";
    private static final String ROLE_TEACHER    = "Teacher";
    private static final String ROLE_CARETAKER  = "Caretaker";
    private static final String ROLE_LANDSCAPER = "landscape gardener";
    private static final String ROLE_PLUMBER    = "plumber";

    /**
     * GIVEN: Role mapping configuration which does not include incoming value as key
     * WHEN: User login via IdP
     * THEN: Incoming role attribute is ignored.
     */
    @Test
    void should_set_no_user_attribute_because_of_no_matching_expression()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList(ROLE_CARETAKER);
        checkPreprocessing(idpRoles, new ArrayList<>());
        checkUserUpdate(idpRoles, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * GIVEN: Role mapping configuration contains a matching regex for the incoming role
     * WHEN: User login via IdP
     * THEN: Incoming role attribute match the regex config and is translated to other value
     */
    @Test
    void should_map_value_because_of_regex_matching_expression()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList("landscape");
        List<String> expectedRoles = Arrays.asList(ROLE_LANDSCAPER);
        checkPreprocessing(idpRoles, expectedRoles);
        checkUserUpdate(idpRoles, expectedRoles, Arrays.asList("somethingElse"));
    }

    /**
     * GIVEN: Role mapping configuration contains a key, which is equal to the incoming value.
     * WHEN: User login via IdP
     * THEN: Incoming role attribute is equal to configuration key and is translated to other value.
     */
    @Test
    void should_map_value_because_of_exact_matching_expression()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList("LERN");
        List<String> expectedRoles = Arrays.asList(ROLE_TEACHER);
        checkPreprocessing(idpRoles, expectedRoles);
        checkUserUpdate(idpRoles, expectedRoles, new ArrayList<>());
    }

    /**
     * GIVEN: Role mapping configuration contains a key, which is equal to the incoming value.
     * WHEN: User login via IdP
     * THEN: Incoming role attribute is not copied, because the role is already assigned to the user.
     */
    @Test
    void should_not_map_value_because_previous_user_role_is_equal()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList("LERN");
        List<String> expectedRoles = Arrays.asList(ROLE_TEACHER);
        checkPreprocessing(idpRoles, expectedRoles);
        checkUserUpdate(idpRoles, expectedRoles, expectedRoles);
    }

    /**
     * GIVEN: Role mapping configuration contains a wildcard expression, which match the incoming value.
     * WHEN: User login via IdP
     * THEN: Incoming role attribute is equal to configuration wildcard key and is translated to other
     * value.
     */
    @Test
    void should_map_value_because_of_wildcard_matching_expression()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList("school");
        List<String> expectedRoles = Arrays.asList(ROLE_TEACHER);
        checkPreprocessing(idpRoles, expectedRoles);
        checkUserUpdate(idpRoles, expectedRoles, new ArrayList<>());
    }

    /**
     * GIVEN: Role mapping configuration contains a wildcard expression, which match the incoming value.
     * WHEN: User login via IdP
     * THEN: Incoming role attribute is equal to configuration wildcard key and is translated to other
     * value.
     */
    @Test
    void should_map_value_because_of_multiple_wildcard_matching_expression()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList("plimbir");
        List<String> expectedRoles = Arrays.asList(ROLE_PLUMBER);
        checkPreprocessing(idpRoles, expectedRoles);
        checkUserUpdate(idpRoles, expectedRoles, new ArrayList<>());
    }

    /**
     * GIVEN: Role mapping configuration contains a valid expression for both incoming roles.
     * WHEN: User login via IdP
     * THEN: Incoming role attributes match configuration keys and two values are translated.
     */
    @Test
    void should_map_two_roles_because_of_two_matching_configurations()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList("landscape", "plimbir");
        List<String> expectedRoles = Arrays.asList(ROLE_LANDSCAPER, ROLE_PLUMBER);
        checkPreprocessing(idpRoles, expectedRoles);
        checkUserUpdate(idpRoles, expectedRoles, new ArrayList<>());
    }

    /**
     * GIVEN: Same role mapping configuration which match both incoming values.
     * WHEN: User login via IdP
     * THEN: Both incoming values are translated but only one role is set to the user attribute, because
     * they have the same translated value.
     */
    @Test
    void should_map_only_one_role_because_both_translated_values_are_equal()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList("school1", "school2");
        List<String> expectedRoles = Arrays.asList(ROLE_TEACHER);
        checkPreprocessing(idpRoles, expectedRoles);
        checkUserUpdate(idpRoles, expectedRoles, new ArrayList<>());
    }

    /**
     * GIVEN: Existing user "role" attribute and an incoming value without a configuration.
     * WHEN: User login via IdP
     * THEN: Incoming value can't be translated and the existing user attribute "role" will be removed.
     */
    @Test
    void should_clear_user_role_attribute_because_of_no_match()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList("doesNotExist");
        List<String> expectedRoles = new ArrayList<>();
        checkPreprocessing(idpRoles, expectedRoles);
        checkUserUpdate(idpRoles, expectedRoles, Arrays.asList(ROLE_TEACHER));
    }

    /**
     * GIVEN: Same role mapping configuration which match two of the three values.
     * WHEN: User login via IdP
     * THEN: Two values are translated and one value will be ignored.
     */
    @Test
    void should_translate_only_two_of_three_values()
        throws DatatypeConfigurationException
    {
        List<String> idpRoles = Arrays.asList("landscape", "plimbir", "doesNoExist");
        List<String> expectedRoles = Arrays.asList(ROLE_LANDSCAPER, ROLE_PLUMBER);
        checkPreprocessing(idpRoles, expectedRoles);
        checkUserUpdate(idpRoles, expectedRoles, new ArrayList<>());
    }

    /**
     * Test preprocessing of a federated identity.
     */
    private void checkPreprocessing(List<String> givenRoles, List<String> expectedRoles)
        throws DatatypeConfigurationException
    {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        //mock mapper model
        IdentityProviderMapperModel mapperModel = Mockito.mock(IdentityProviderMapperModel.class);
        when(mapperModel.getConfig()).thenReturn(getContextUserAttribute());
        when(mapperModel.getConfigMap(anyString())).thenReturn(getMappedValuesConfig());
        //mock identity context
        BrokeredIdentityContext context = Mockito.mock(BrokeredIdentityContext.class);
        when(context.getContextData()).thenReturn(mockContextData(givenRoles, givenRoles));
        //capture method arguments
        ArgumentCaptor<String> attributeKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<String>> attributeValues = ArgumentCaptor.forClass(List.class);
        doNothing().when(context).setUserAttribute(attributeKey.capture(), attributeValues.capture());
        //call mappers preprocessing
        MappedValueUserAttributeMapper mapper = new MappedValueUserAttributeMapper();
        mapper.preprocessFederatedIdentity(session, realm, mapperModel, context);
        //verify result
        Assertions.assertEquals(ROLE_ATTR_KEY, attributeKey.getValue());
        Assertions.assertTrue(CollectionUtil.collectionEquals(givenRoles, attributeValues.getAllValues().get(0)));
        if (expectedRoles.isEmpty()) {
            Assertions.assertEquals(true, attributeKey.getAllValues().size() == 2);
            Assertions.assertTrue(attributeValues.getAllValues().get(1).isEmpty());
        }
        else if (CollectionUtil.collectionEquals(givenRoles, expectedRoles)) {
            Assertions.assertEquals(true, attributeKey.getAllValues().size() == 1);
            Assertions.assertEquals(true, attributeValues.getAllValues().size() == 1);
        }
        else {
            Assertions.assertEquals(ROLE_ATTR_KEY, attributeKey.getAllValues().get(1));
            Assertions.assertTrue(CollectionUtil.collectionEquals(expectedRoles, attributeValues.getAllValues().get(1)));
        }
    }

    /**
     * Test update brokered user.
     */
    private void checkUserUpdate(List<String> givenRoles, List<String> expectedRoles, List<String> previousUserRole)
        throws DatatypeConfigurationException
    {
        KeycloakSession session = mock(KeycloakSession.class);
        RealmModel realm = mock(RealmModel.class);
        //mock mapper model
        IdentityProviderMapperModel mapperModel = Mockito.mock(IdentityProviderMapperModel.class);
        when(mapperModel.getConfig()).thenReturn(getContextUserAttribute());
        when(mapperModel.getConfigMap(anyString())).thenReturn(getMappedValuesConfig());
        //mock identity context
        BrokeredIdentityContext context = Mockito.mock(BrokeredIdentityContext.class);
        when(context.getContextData()).thenReturn(mockContextData(givenRoles, expectedRoles));
        //mock user
        UserModel user = Mockito.mock(UserModel.class);
        when(user.getAttributes()).thenReturn(getUserAttribute(previousUserRole));
        ArgumentCaptor<String> attributeNameSet = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<String>> userAttrValues = ArgumentCaptor.forClass(List.class);
        doNothing().when(user).setAttribute(attributeNameSet.capture(), userAttrValues.capture());
        //call mappers user update
        MappedValueUserAttributeMapper mapper = new MappedValueUserAttributeMapper();
        mapper.updateBrokeredUser(session, realm, user, mapperModel, context);
        //verify result

        if (!CollectionUtil.collectionEquals(expectedRoles, previousUserRole)) {
            Assertions.assertTrue(CollectionUtil.collectionEquals(givenRoles, userAttrValues.getAllValues().get(0)));
            Assertions.assertTrue(CollectionUtil.collectionEquals(expectedRoles, userAttrValues.getAllValues().get(1)));
        }
        else if (CollectionUtil.collectionEquals(expectedRoles, previousUserRole)) {
            Assertions.assertEquals(true, attributeNameSet.getAllValues().size() == 1);
            Assertions.assertEquals(true, userAttrValues.getAllValues().size() == 1);
        }
        else if (CollectionUtil.collectionEquals(givenRoles, previousUserRole) &&
                CollectionUtil.collectionEquals(givenRoles, expectedRoles)) {
            //no user changes
            Assertions.assertEquals(0, userAttrValues.getAllValues().size());
        }
        else {
            Assertions.assertEquals(1, userAttrValues.getAllValues().size());
            Assertions.assertTrue(CollectionUtil.collectionEquals(expectedRoles, userAttrValues.getAllValues().get(0)));
        }
    }

    /**
     * Get attribute value mapping configuration.
     */
    private Map<String, String> getMappedValuesConfig()
    {
        HashMap<String, String> config = new HashMap<>();
        config.put("LERN", ROLE_TEACHER);
        config.put("school*", ROLE_TEACHER);
        config.put("REGEX(^landscape.*)", ROLE_LANDSCAPER);
        config.put("pl?mb*", ROLE_PLUMBER);
        return config;
    }

    /**
     * Get user attribute for broker identity context.
     */
    private Map<String, String> getContextUserAttribute()
    {
        HashMap<String, String> attribute = new HashMap<>();
        attribute.put(UserAttributeMapper.ATTRIBUTE_NAME, ROLE_ATTR_KEY);
        attribute.put(UserAttributeMapper.USER_ATTRIBUTE, ROLE_ATTR_KEY);
        return attribute;
    }

    /**
     * Get user attribute for user object.
     */
    private Map<String, List<String>> getUserAttribute(List<String> values)
    {
        Map<String, List<String>> attributes = new HashMap<>();
        if (values != null) {
            attributes.put(ROLE_ATTR_KEY, values);
        }
        return attributes;
    }

    /**
     * Mock identity broker context data.
     */
    private Map<String, Object> mockContextData(List<String> roles, List<String> userRoles)
        throws DatatypeConfigurationException
    {
        HashMap<String, Object> contextData = new HashMap<>();
        GregorianCalendar gc = new GregorianCalendar();
        // giving current date and time to gc
        gc.setTime(new Date());
        AssertionType at = new AssertionType("1", DatatypeFactory.newInstance().newXMLGregorianCalendar(gc));
        at.addStatement(getAttribute(ROLE_ATTR_KEY, roles));
        contextData.put(SAMLEndpoint.SAML_ASSERTION, at);
        //add user attribute
        contextData.put(Constants.USER_ATTRIBUTES_PREFIX + ROLE_ATTR_KEY, userRoles);
        return contextData;
    }

    /**
     * Get attribute statement type for broker identity context.
     */
    private AttributeStatementType getAttribute(String name, List<String> values)
    {
        AttributeStatementType ast = new AttributeStatementType();
        AttributeType attributeType = new AttributeType(name);
        for (String value : values) {
            attributeType.addAttributeValue(value);
        }
        AttributeStatementType.ASTChoiceType astChoiceType = new AttributeStatementType.ASTChoiceType(attributeType);
        ast.addAttribute(astChoiceType);
        return ast;
    }
}
