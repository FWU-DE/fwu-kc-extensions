package de.intension.id.saml;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import de.intension.id.oidc.PrefixAttributeOidcMapper;

class PrefixAttributeSamlMapperTest
{

    @ParameterizedTest
    @CsvSource({
            ",PREFIX.1234",
            "false,PREFIX.1234",
            "true,prefix.1234"
    })
    @SuppressWarnings("unchecked")
    void should_map_school_id_with_prefix(Boolean lowercase, String expected)
    {
        ArgumentCaptor<List<String>> prefixCaptor = ArgumentCaptor.forClass(List.class);
        var token = samlStatement("schoolId", "1234");
        var config = mapperConfig("schoolId", "prefixedId", "PREFIX.", lowercase);

        var context = testMapping(token, config);

        Mockito.verify(context, times(1)).setUserAttribute(Mockito.matches("prefixedId"), prefixCaptor.capture());
        assertThat(String.format("Expected %s for lowercase %b", expected, lowercase), prefixCaptor.getValue().get(0), equalTo(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "1234##4567,,2,PREFIX.1234##PREFIX.4567",
            "1234##4567,false,2,PREFIX.1234##PREFIX.4567",
            "1234##4567,true,2,prefix.1234##prefix.4567",
            "##4567,false,1,PREFIX.4567",
            "  ##4567,false,1,PREFIX.4567",
            "1234##,false,1,PREFIX.1234"
    })
    @SuppressWarnings("unchecked")
    void should_map_multiple_school_ids_with_prefix(String claimValue, Boolean lowercase, Integer expectedAmount, String expected)
    {
        ArgumentCaptor<List<String>> prefixCaptor = ArgumentCaptor.forClass(List.class);
        var token = samlStatement("schoolId", Arrays.asList(claimValue.split("##")));
        var config = mapperConfig("schoolId", "prefixedId", "PREFIX.", lowercase);

        var context = testMapping(token, config);

        Mockito.verify(context, times(1)).setUserAttribute(Mockito.matches("prefixedId"), prefixCaptor.capture());
        List<String> actual = prefixCaptor.getValue();
        assertThat(actual, hasSize(expectedAmount));
        assertThat(String.format("Expected %s for lowercase %b", expected, lowercase), actual, contains(expected.split("##")));
    }

    @Test
    void should_not_map_school_id_if_saml_statement_misses_value()
    {
        var token = samlStatement("foo", "bar");
        var config = mapperConfig("schoolId", "prefixedId", "NOT ", true);

        var context = testMapping(token, config);

        Mockito.verify(context, Mockito.atMostOnce()).getContextData();
        Mockito.verify(context, Mockito.never()).setUserAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_not_map_school_id_if_saml_statement_value_is_invalid(String value)
    {
        var token = samlStatement("school", value);
        var config = mapperConfig("school", "prefixed", "NOT ", true);

        var context = testMapping(token, config);

        Mockito.verify(context, Mockito.atMostOnce()).getContextData();
        Mockito.verify(context, Mockito.never()).setUserAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @Test
    void should_not_map_school_ids_if_saml_statement_is_empty_list()
    {
        var token = samlStatement("school", List.of());
        var config = mapperConfig("school", "prefixed", "NOT ", true);

        var context = testMapping(token, config);

        Mockito.verify(context, Mockito.atMostOnce()).getContextData();
        Mockito.verify(context, Mockito.never()).setUserAttribute(Mockito.anyString(), Mockito.anyList());
    }

    private Map<String, String> mapperConfig(String samlValue, String userAttribute, String prefix, Boolean lowercase)
    {
        var config = new HashMap<>(Map.of(UserAttributeMapper.ATTRIBUTE_NAME, samlValue,
                                          UserAttributeMapper.USER_ATTRIBUTE, userAttribute,
                                          PrefixAttributeOidcMapper.PREFIX, prefix));
        if (lowercase != null) {
            config.put(PrefixAttributeOidcMapper.LOWER_CASE, Boolean.toString(lowercase));
        }
        return config;
    }

    private AssertionType samlStatement(String attributeName, Object value)
    {
        var statement = Mockito.mock(AssertionType.class);
        var attributeValue = new AttributeType(attributeName);
        attributeValue.addAttributeValue(value);
        var attribute = new AttributeStatementType();
        attribute.addAttribute(new AttributeStatementType.ASTChoiceType(attributeValue));
        var attributeStatements = Set.of(attribute);
        Mockito.when(statement.getAttributeStatements()).thenReturn(attributeStatements);
        return statement;
    }

    private BrokeredIdentityContext testMapping(AssertionType samlAssertion, Map<String, String> mapperConfig)
    {
        var context = Mockito.mock(BrokeredIdentityContext.class);
        Mockito.when(context.getContextData()).thenReturn(Map.of(SAMLEndpoint.SAML_ASSERTION, samlAssertion));
        var mapperModel = Mockito.mock(IdentityProviderMapperModel.class);
        Mockito.when(mapperModel.getConfig()).thenReturn(mapperConfig);
        new PrefixAttributeSamlMapper().importNewUser(null, null, null, mapperModel, context);
        return context;
    }
}
