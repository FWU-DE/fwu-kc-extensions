package de.intension.api.enumerations;

public enum GermanBoolean
{

    JA,
    NEIN;

    public static GermanBoolean fromBoolean(Boolean bool) {
        if (bool == null) {
            return null;
        }
        if (bool) {
            return JA;
        }
        return NEIN;
    }
    private static String prettyPrint;

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (GermanBoolean germanBoolean : GermanBoolean.values()) {
                sb.append(germanBoolean.name()).append(" (").append(germanBoolean).append("), ");
            }
            String temp = sb.toString();
            prettyPrint = temp.substring(0, temp.length() - 2);
        }
        return prettyPrint;
    }
}
