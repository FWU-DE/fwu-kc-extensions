package de.intension.acronym;

public final class AcronymUtil {

    private AcronymUtil() {
        throw new UnsupportedOperationException("Utility class must be accessed in a static way");
    }

    /**
     * Combine first two letters of the given names to a lowercase string.
     */
    public static String createAcronym(String firstName, String lastName) {
        return createAcronym(firstName, lastName, null);
    }

    /**
     * Combine first two letters of the given names to a string.
     */
    public static String createAcronym(String firstName, String lastName, String modifier) {
        String acronym;
        if (AcronymMapper.MODIFIER_CAMEL_CASE.equals(modifier)) {
            acronym = firstCharToUpperCase(shorten(firstName)) + firstCharToUpperCase(shorten(lastName));
        }
        else {
            //lower case
            acronym = shorten(firstName) + shorten(lastName);
            acronym = acronym.toLowerCase();
        }
        return acronym;
    }

    private static String shorten(String value) {
        if (value == null) {
            value = "";
        }
        return value.length() <= 2 ? value : value.substring(0, 2);
    }

    private static String firstCharToUpperCase(String value)
    {
        if (value.length() == 1) {
            value = value.toUpperCase();
        }
        else if (value.length() > 1) {
            value = value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
        }
        return value;
    }
}
