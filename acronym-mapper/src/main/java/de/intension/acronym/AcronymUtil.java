package de.intension.acronym;

public final class AcronymUtil {

    private AcronymUtil() {
        throw new UnsupportedOperationException("Utility class must be accessed in a static way");
    }

    /**
     * Combine first two letters of the given names to a lowercase string.
     */
    public static String createAcronym(String firstName, String lastName) {
        var acronym = shorten(firstName) + shorten(lastName);
        return acronym.toLowerCase();
    }

    private static String shorten(String value) {
        if (value == null) {
            value = "";
        }
        return value.length() <= 2 ? value : value.substring(0, 2);
    }
}
