package de.intension.api.enumerations;

public enum PersonenStatus
{

    AKTIV,
    INAKTIV;

    private static String prettyPrint;

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (PersonenStatus ps : PersonenStatus.values()) {
                sb.append(ps.name()).append(", ");
            }
            String temp = sb.toString();
            prettyPrint = temp.substring(0, temp.length() - 2);
        }
        return prettyPrint;
    }
}
