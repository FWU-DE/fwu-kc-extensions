package de.intension.id;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
}
