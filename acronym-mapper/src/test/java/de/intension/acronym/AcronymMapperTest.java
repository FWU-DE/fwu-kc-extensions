package de.intension.acronym;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.UserModel;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AcronymMapperTest
{

    private static final String ATTRIBUTE_DEFAULT_VALUE = "acronym";

    /**
     * Test for successful case
     */
    @Test
    void should_map_names_to_acronym() {
        ArgumentCaptor<String> acronymCaptor = ArgumentCaptor.forClass(String.class);
        var user = user("John", "Wick");

        testMapping(user);

        // called in the mapper to set the acronym value
        Mockito.verify(user, times(1)).setSingleAttribute(Mockito.matches(ATTRIBUTE_DEFAULT_VALUE), acronymCaptor.capture());
        // called in the test method to check the value
        assertThat(acronymCaptor.getValue(), equalTo("jowi"));
    }

    /**
     * Test for successful case
     */
    @Test
    void should_map_names_to_acronym_camelCase()
    {
        ArgumentCaptor<String> acronymCaptor = ArgumentCaptor.forClass(String.class);
        var user = user("John", "Wick");

        testMapping(user, null, AcronymMapper.MODIFIER_CAMEL_CASE);

        // called in the mapper to set the acronym value
        Mockito.verify(user, times(1)).setSingleAttribute(Mockito.matches(ATTRIBUTE_DEFAULT_VALUE), acronymCaptor.capture());
        // called in the test method to check the value
        assertThat(acronymCaptor.getValue(), equalTo("JoWi"));
    }

    /**
     * Test for successful case and custom mapper config
     */
    @Test
    void should_map_names_to_custom_attribute() {
        ArgumentCaptor<String> acronymCaptor = ArgumentCaptor.forClass(String.class);
        var user = user("Peter", "Jackson");

        testMapping(user, "kürzel", null);

        // called in the mapper to set the acronym value
        Mockito.verify(user, times(1)).setSingleAttribute(Mockito.matches("kürzel"), acronymCaptor.capture());
        // called in the test method to check the value
        assertThat(acronymCaptor.getValue(), equalTo("peja"));
    }

    /**
     * Test for successful case and custom mapper config
     */
    @Test
    void should_map_names_to_custom_attribute_camelCase()
    {
        ArgumentCaptor<String> acronymCaptor = ArgumentCaptor.forClass(String.class);
        var user = user("Peter", "Jackson");

        testMapping(user, "kürzel", AcronymMapper.MODIFIER_CAMEL_CASE);

        // called in the mapper to set the acronym value
        Mockito.verify(user, times(1)).setSingleAttribute(Mockito.matches("kürzel"), acronymCaptor.capture());
        // called in the test method to check the value
        assertThat(acronymCaptor.getValue(), equalTo("PeJa"));
    }

    /**
     * Test for error cases
     */
    @ParameterizedTest
    @CsvSource({
            "M,Shorty",
            "Short,S",
            ",None",
            "No,",
            ","
    })
    void should_not_set_attribute_value(String firstName, String lastName) {
        var user = user(firstName, lastName);

        testMapping(user);

        Mockito.verify(user, times(1)).getFirstName();
        Mockito.verify(user, times(1)).getLastName();
        Mockito.verify(user, never()).setSingleAttribute(any(), any());
    }

    /**
     * Test for error cases
     */
    @ParameterizedTest
    @CsvSource({
            "M,Shorty",
            "Short,S",
            ",None",
            "No,",
            ","
    })
    void should_not_set_attribute_value_camelCase(String firstName, String lastName)
    {
        var user = user(firstName, lastName);

        testMapping(user, null, null);

        Mockito.verify(user, times(1)).getFirstName();
        Mockito.verify(user, times(1)).getLastName();
        Mockito.verify(user, never()).setSingleAttribute(any(), any());
    }

    /**
     * Mock a user with given first and last name.
     */
    private UserModel user(String firstName, String lastName) {
        var user = Mockito.mock(UserModel.class);
        Mockito.when(user.getFirstName()).thenReturn(firstName);
        Mockito.when(user.getLastName()).thenReturn(lastName);
        return user;
    }

    /**
     * Mock a call to the {@link AcronymMapper}.
     *
     * @return User with attribute 'acronym' containing acronym.
     */
    private void testMapping(UserModel user) {
        testMapping(user, null, null);
    }

    /**
     * Mock a call to the {@link AcronymMapper}.
     *
     * @return User with given attribute containing acronym.
     */
    private void testMapping(UserModel user, String attribute, String modifier) {
        Map<String, String> config = new HashMap<>();
        if (attribute != null) {
            config.put(AcronymMapper.ATTRIBUTE, attribute);
        }
        if (modifier != null) {
            config.put(AcronymMapper.MODIFIER, modifier);
        }

        var mapperModel = Mockito.mock(IdentityProviderMapperModel.class);
        Mockito.when(mapperModel.getConfig()).thenReturn(config);

        // call the mapper (any method of the two is fine)
        new AcronymMapper().updateBrokeredUser(null, null, user, mapperModel, null);
    }
}
