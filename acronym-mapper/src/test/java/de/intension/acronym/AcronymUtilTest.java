package de.intension.acronym;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AcronymUtilTest {

    /**
     * GIVEN a first and a last name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of the two names is returned as expected
     */
    @Test
    void should_combine_names_to_acronym() {
        var firstName = "Peter";
        var lastName = "Zwegat";

        var acronym = AcronymUtil.createAcronym(firstName, lastName);

        Assertions.assertEquals(acronym, "pezw");
        Assertions.assertNotEquals(acronym, "PeZw");
    }

    /**
     * GIVEN a first and a last name
     * AND the first name is one character long
     * WHEN calling the acronym creating method
     * THEN an acronym made up of the two names is returned as expected
     */
    @Test
    void should_combine_short_first_name_to_acronym() {
        var firstName = "O";
        var lastName = "Zhong";

        var acronym = AcronymUtil.createAcronym(firstName, lastName);

        Assertions.assertEquals(acronym, "ozh");
    }

    /**
     * GIVEN a first and a last name
     * AND the last name is one character long
     * WHEN calling the acronym creating method
     * THEN an acronym made up of the two names is returned as expected
     */
    @Test
    void should_combine_short_last_name_to_acronym() {
        var firstName = "Phil";
        var lastName = "P";

        var acronym = AcronymUtil.createAcronym(firstName, lastName);

        Assertions.assertEquals(acronym, "php");
    }

    /**
     * GIVEN a last name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of only the last name is returned as expected
     */
    @Test
    void should_combine_missing_first_name_to_acronym() {
        var lastName = "Mueller";

        var acronym = AcronymUtil.createAcronym(null, lastName);

        Assertions.assertEquals(acronym, "mu");
    }

    /**
     * GIVEN a first name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of only the first name is returned as expected
     */
    @Test
    void should_combine_missing_last_name_to_acronym() {
        var firstName = "Fiona";

        var acronym = AcronymUtil.createAcronym(firstName, null);

        Assertions.assertEquals(acronym, "fi");
    }

    /**
     * GIVEN an empty first name and a valid last name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of only the last name is returned as expected
     */
    @Test
    void should_combine_empty_first_name_to_acronym() {
        var firstName = "";
        var lastName = "Mueller";

        var acronym = AcronymUtil.createAcronym(firstName, lastName);

        Assertions.assertEquals(acronym, "mu");
    }

    /**
     * GIVEN a first name and an empty last name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of only the first name is returned as expected
     */
    @Test
    void should_combine_empty_last_name_to_acronym() {
        var firstName = "Fiona";
        var lastName = "";

        var acronym = AcronymUtil.createAcronym(firstName, lastName);

        Assertions.assertEquals(acronym, "fi");
    }
}
