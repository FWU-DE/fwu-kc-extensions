package de.intension.id.saml;

import de.intension.id.PrefixAttributeConstants;
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
import org.keycloak.models.UserModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.times;

class PrefixAttributeSamlMapperTest {

    @ParameterizedTest
    @CsvSource({
            ",PREFIX.1234",
            "false,PREFIX.1234",
            "true,prefix.1234"
    })
    @SuppressWarnings("unchecked")
    void should_map_school_id_with_prefix(Boolean lowercase, String expected) {
        ArgumentCaptor<List<String>> prefixCaptor = ArgumentCaptor.forClass(List.class);
        var token = samlStatement("schoolId", "1234");
        var config = mapperConfig("schoolId", "prefixedId", "PREFIX.", lowercase);

        var user = testMapping(token, config);

        Mockito.verify(user, times(1)).setAttribute(Mockito.matches("prefixedId"), prefixCaptor.capture());
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
    void should_map_multiple_school_ids_with_prefix(String claimValue, Boolean lowercase, Integer expectedAmount, String expected) {
        ArgumentCaptor<List<String>> prefixCaptor = ArgumentCaptor.forClass(List.class);
        var token = samlStatement("schoolId", Arrays.asList(claimValue.split("##")));
        var config = mapperConfig("schoolId", "prefixedId", "PREFIX.", lowercase);

        var user = testMapping(token, config);

        Mockito.verify(user, times(1)).setAttribute(Mockito.matches("prefixedId"), prefixCaptor.capture());
        List<String> actual = prefixCaptor.getValue();
        assertThat(actual, hasSize(expectedAmount));
        assertThat(String.format("Expected %s for lowercase %b", expected, lowercase), actual, contains(expected.split("##")));
    }

    @Test
    void should_not_map_school_id_if_saml_statement_misses_value() {
        var token = samlStatement("foo", "bar");
        var config = mapperConfig("schoolId", "prefixedId", "NOT ", true);

        var user = testMapping(token, config);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_not_map_school_id_if_saml_statement_value_is_invalid(String value) {
        var token = samlStatement("school", value);
        var config = mapperConfig("school", "prefixed", "NOT ", true);

        var user = testMapping(token, config);

        Mockito.verify(user, Mockito.never()).setAttribute(Mockito.anyString(), Mockito.anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void should_map_school_id_for_friendly_name() {
        ArgumentCaptor<List<String>> prefixCaptor = ArgumentCaptor.forClass(List.class);
        var token = samlStatement("schoolId", "1234");
        var config = new HashMap<>(Map.of(UserAttributeMapper.ATTRIBUTE_FRIENDLY_NAME, "schoolId",
                UserAttributeMapper.USER_ATTRIBUTE, "prefixedId",
                PrefixAttributeConstants.PREFIX, "PREFIX.",
                PrefixAttributeConstants.LOWER_CASE, "false"));

        var user = testMapping(token, config);

        Mockito.verify(user, times(1)).setAttribute(Mockito.matches("prefixedId"), prefixCaptor.capture());
        assertThat(prefixCaptor.getValue().get(0), equalTo("PREFIX.1234"));
    }

    private Map<String, String> mapperConfig(String samlValue, String userAttribute, String prefix, Boolean lowercase) {
        var config = new HashMap<>(Map.of(UserAttributeMapper.ATTRIBUTE_NAME, samlValue,
                UserAttributeMapper.USER_ATTRIBUTE, userAttribute,
                PrefixAttributeConstants.PREFIX, prefix));
        if (lowercase != null) {
            config.put(PrefixAttributeConstants.LOWER_CASE, Boolean.toString(lowercase));
        }
        return config;
    }

    @SuppressWarnings("unchecked")
    private AssertionType samlStatement(String attributeName, Object value) {
        var statement = Mockito.mock(AssertionType.class);
        var attributeValue = new AttributeType(attributeName);
        attributeValue.setFriendlyName(attributeName);
        if (value instanceof List) {
            for (Object item : (List<Object>) value) {
                attributeValue.addAttributeValue(item);
            }
        } else {
            attributeValue.addAttributeValue(value);
        }
        var attribute = new AttributeStatementType();
        attribute.addAttribute(new AttributeStatementType.ASTChoiceType(attributeValue));
        var attributeStatements = Set.of(attribute);
        Mockito.when(statement.getAttributeStatements()).thenReturn(attributeStatements);
        return statement;
    }

    private UserModel testMapping(AssertionType samlAssertion, Map<String, String> mapperConfig) {
        var user = Mockito.spy(UserModel.class);

        var context = Mockito.mock(BrokeredIdentityContext.class);
        Mockito.when(context.getContextData()).thenReturn(Map.of(SAMLEndpoint.SAML_ASSERTION, samlAssertion));
        var mapperModel = Mockito.mock(IdentityProviderMapperModel.class);
        Mockito.when(mapperModel.getConfig()).thenReturn(mapperConfig);
        new PrefixAttributeSamlMapper().importNewUser(null, null, user, mapperModel, context);
        return user;
    }
}
