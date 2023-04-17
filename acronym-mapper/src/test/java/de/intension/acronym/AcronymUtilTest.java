package de.intension.acronym;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

        Assertions.assertEquals("pezw", acronym);
        Assertions.assertNotEquals("PeZw", acronym);
    }

    /**
     * GIVEN a first and a last name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of the two names is returned as expected
     */
    @ParameterizedTest
    @CsvSource({
            "Peter,Zwegat",
            "peter,zwegat",
            "pETER,zWEGAT"
    })
    void should_combine_names_to_acronym_camelCase(String firstName, String lastName)
    {
        var acronym = AcronymUtil.createAcronym(firstName, lastName, AcronymMapper.MODIFIER_CAMEL_CASE);

        Assertions.assertEquals("PeZw", acronym);
        Assertions.assertNotEquals("pezw", acronym);
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

        Assertions.assertEquals("ozh", acronym);
    }

    /**
     * GIVEN a first and a last name
     * AND the first name is one character long
     * WHEN calling the acronym creating method
     * THEN an acronym made up of the two names is returned as expected
     */
    @ParameterizedTest
    @CsvSource({
            "O,Zhong",
            "o,zhong",
            "o,zHONG"
    })
    void should_combine_short_first_name_to_acronym_camelCase(String firstName, String lastName)
    {
        var acronym = AcronymUtil.createAcronym(firstName, lastName, AcronymMapper.MODIFIER_CAMEL_CASE);

        Assertions.assertEquals("OZh", acronym);
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

        Assertions.assertEquals("php", acronym);
    }

    /**
     * GIVEN a first and a last name
     * AND the last name is one character long
     * WHEN calling the acronym creating method
     * THEN an acronym made up of the two names is returned as expected
     */
    @ParameterizedTest
    @CsvSource({
            "Phil,P",
            "phil,p",
            "pHIL,p"
    })
    void should_combine_short_last_name_to_acronym_camelCase(String firstName, String lastName)
    {
        var acronym = AcronymUtil.createAcronym(firstName, lastName, AcronymMapper.MODIFIER_CAMEL_CASE);

        Assertions.assertEquals("PhP", acronym);
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

        Assertions.assertEquals("mu", acronym);
    }

    /**
     * GIVEN a last name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of only the last name is returned as expected
     */
    @Test
    void should_combine_missing_first_name_to_acronym_camelCase()
    {
        var lastName = "Mueller";

        var acronym = AcronymUtil.createAcronym(null, lastName, AcronymMapper.MODIFIER_CAMEL_CASE);

        Assertions.assertEquals("Mu", acronym);
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

        Assertions.assertEquals("fi", acronym);
    }

    /**
     * GIVEN a first name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of only the first name is returned as expected
     */
    @Test
    void should_combine_missing_last_name_to_acronym_camelCase()
    {
        var firstName = "Fiona";

        var acronym = AcronymUtil.createAcronym(firstName, null, AcronymMapper.MODIFIER_CAMEL_CASE);

        Assertions.assertEquals("Fi", acronym);
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

        Assertions.assertEquals("mu", acronym);
    }

    /**
     * GIVEN an empty first name and a valid last name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of only the last name is returned as expected
     */
    @Test
    void should_combine_empty_first_name_to_acronym_camelCase()
    {
        var firstName = "";
        var lastName = "Mueller";

        var acronym = AcronymUtil.createAcronym(firstName, lastName, AcronymMapper.MODIFIER_CAMEL_CASE);

        Assertions.assertEquals("Mu", acronym);
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

        Assertions.assertEquals("fi", acronym);
    }

    /**
     * GIVEN a first name and an empty last name
     * WHEN calling the acronym creating method
     * THEN an acronym made up of only the first name is returned as expected
     */
    @Test
    void should_combine_empty_last_name_to_acronym_camelCase()
    {
        var firstName = "Fiona";
        var lastName = "";

        var acronym = AcronymUtil.createAcronym(firstName, lastName, AcronymMapper.MODIFIER_CAMEL_CASE);

        Assertions.assertEquals("Fi", acronym);
    }
}
