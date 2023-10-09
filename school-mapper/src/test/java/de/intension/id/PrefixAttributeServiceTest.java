package de.intension.id;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.fail;

class PrefixAttributeServiceTest {

    @Test
    void should_prefix_single_value() {
        var prefix = "DE-BY-Schullogin.";

        var prefixed = new PrefixAttributeService(prefix).prefix("8901");

        assertThat(prefixed, equalTo("DE-BY-Schullogin.8901"));
    }

    @Test
    void should_not_prefix_single_value_for_missing_prefix() {
        try {
            new PrefixAttributeService(null).prefix("8901");
            fail("Should not work with null prefix");
        } catch (UnsupportedOperationException uoe) {
            assertThat(uoe.getMessage(), containsString("Prefix cannot be empty"));
        }
    }

    @Test
    void should_not_prefix_missing_value() {
        var prefix = "DE-BY-Schullogin.";

        var prefixed = new PrefixAttributeService(prefix).prefix("");

        assertThat(prefixed, emptyString());
    }

    @Test
    void should_prefix_list_of_values() {
        var prefix = "DE-BY-Schullogin.";

        var prefixed = new PrefixAttributeService(prefix).prefix(List.of("teckel", "example"));

        assertThat(prefixed, iterableWithSize(2));
        assertThat(prefixed, contains("DE-BY-Schullogin.teckel", "DE-BY-Schullogin.example"));
    }

    @Test
    void should_not_prefix_values_where_one_is_null() {
        var prefix = "DE-BY-Schullogin.";
        List<String> values = new ArrayList<>();
        values.add("example");
        values.add(null);

        var prefixed = new PrefixAttributeService(prefix).prefix(values);

        assertThat(prefixed, iterableWithSize(1));
        assertThat(prefixed, contains(equalTo("DE-BY-Schullogin.example")));
    }

    @Test
    void should_prefix_extracted_single_value(){
        String prefix = "DE-BY-Schullogin.";
        String prefixed = new PrefixAttributeService(prefix, false, "^ou=([^,]*),.*").prefix("ou=school_id_103,o=something,c=de");

        assertThat(prefixed, equalTo("DE-BY-Schullogin.school_id_103"));
    }

    @Test
    void should_prefix_extracted_single_value_with_lower_case(){
        String prefix = "DE-BY-Schullogin.";
        String prefixed = new PrefixAttributeService(prefix, true, "^ou=([^,]*),.*").prefix("ou=school_id_103,o=something,c=de");

        assertThat(prefixed, equalTo("de-by-schullogin.school_id_103"));
    }

    @Test
    void should_prefix_extracted_list_of_values() {
        var prefix = "DE-BY-Schullogin.";

        var prefixed = new PrefixAttributeService(prefix, false, "^ou=([^,]*),.*").prefix(List.of("ou=school_id_103,o=something,c=de", "ou=school_id_503,o=something,c=de"));

        assertThat(prefixed, iterableWithSize(2));
        assertThat(prefixed, contains("DE-BY-Schullogin.school_id_103", "DE-BY-Schullogin.school_id_503"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void should_prefix_only_if_regex_is_empty_or_null(String regex){
        var prefix = "DE-BY-Schullogin.";

        var prefixed = new PrefixAttributeService(prefix, false, regex).prefix(List.of("ou=school_id_103,o=something,c=de", "ou=school_id_503,o=something,c=de"));

        assertThat(prefixed, iterableWithSize(2));
        assertThat(prefixed, contains("DE-BY-Schullogin.ou=school_id_103,o=something,c=de", "DE-BY-Schullogin.ou=school_id_503,o=something,c=de"));
    }

    @Test
    void should_not_return_prefixed_value_because_of_no_matching_value_available(){
        String prefix = "DE-BY-Schullogin.";
        String prefixed = new PrefixAttributeService(prefix, false, "^ou=([^,]*),.*").prefix("oa=school_id_103,o=something,c=de");

        Assertions.assertNull(prefixed);
    }

    @Test
    void should_throw_exception_because_of_invalid_regex(){
        String prefix = "DE-BY-Schullogin.";
        Assertions.assertThrows(PatternSyntaxException.class, () -> {
            new PrefixAttributeService(prefix, false, "^ou=([[[^,]*),.*");
        });
    }


}
