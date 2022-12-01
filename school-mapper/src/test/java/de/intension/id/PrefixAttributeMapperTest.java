package de.intension.id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProvider;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.times;

class PrefixAttributeMapperTest {

    @ParameterizedTest
    @CsvSource({
            ",PREFIX.1234",
            "false,PREFIX.1234",
            "true,prefix.1234"
    })
    void should_map_school_id_with_prefix(Boolean lowercase, String expected) {
        ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
        var token = token("schoolId", "1234");
        var config = mapperConfig("schoolId", "prefixedId", "PREFIX.", lowercase);

        var user = testMapping(token, config);

        Mockito.verify(user, times(1)).setSingleAttribute(Mockito.matches("prefixedId"), prefixCaptor.capture());
        assertThat(String.format("Expected %s for lowercase %b", expected, lowercase), prefixCaptor.getValue(), equalTo(expected));
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
        var token = token("schoolId", Arrays.asList(claimValue.split("##")));
        var config = mapperConfig("schoolId", "prefixedId", "PREFIX.", lowercase);

        var user = testMapping(token, config);

        Mockito.verify(user, times(1)).setAttribute(Mockito.matches("prefixedId"), prefixCaptor.capture());
        List<String> actual = prefixCaptor.getValue();
        assertThat(actual, hasSize(expectedAmount));
        assertThat(String.format("Expected %s for lowercase %b", expected, lowercase), actual, contains(expected.split("##")));
    }

    @Test
    void should_not_map_school_id_if_token_misses_claim() {
        var token = new JsonWebToken();
        var config = mapperConfig("schoolId", "prefixedId", "NOT ", true);

        var user = testMapping(token, config);

        Mockito.verifyNoInteractions(user);
    }

    private static final String NULL_VALUE = null;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_not_map_school_id_if_claim_value_is_invalid(String value) {
        var token = token("school", value);
        var config = mapperConfig("school", "prefixed", "NOT ", true);

        var user = testMapping(token, config);

        Mockito.verifyNoInteractions(user);
    }

    @Test
    void should_not_map_school_ids_if_claim_value_is_empty_list() {
        var token = token("school", List.of());
        var config = mapperConfig("school", "prefixed", "NOT ", true);

        var user = testMapping(token, config);

        Mockito.verifyNoInteractions(user);
    }

    private Map<String, String> mapperConfig(String claim, String attribute, String prefix, Boolean lowercase) {
        var config = new HashMap<>(Map.of(AbstractClaimMapper.CLAIM, claim,
                PrefixAttributeMapper.ATTRIBUTE, attribute,
                PrefixAttributeMapper.PREFIX, prefix));
        if (lowercase != null) {
            config.put(PrefixAttributeMapper.LOWER_CASE, Boolean.toString(lowercase));
        }
        return config;
    }

    private JsonWebToken token(String claim, Object value) {
        var token = new JsonWebToken();
        token.setOtherClaims(claim, value);
        return token;
    }

    private UserModel testMapping(JsonWebToken token, Map<String, String> mapperConfig) {
        var context = Mockito.mock(BrokeredIdentityContext.class);
        Mockito.when(context.getContextData()).thenReturn(Map.of(KeycloakOIDCIdentityProvider.VALIDATED_ACCESS_TOKEN, token));
        var mapperModel = Mockito.mock(IdentityProviderMapperModel.class);
        Mockito.when(mapperModel.getConfig()).thenReturn(mapperConfig);
        var user = Mockito.mock(UserModel.class);
        new PrefixAttributeMapper().updateBrokeredUser(null, null, user, mapperModel, context);
        return user;
    }
}
