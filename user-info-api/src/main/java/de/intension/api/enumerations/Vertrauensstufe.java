package de.intension.api.enumerations;

public enum Vertrauensstufe
{

    KEIN("Keine"),
    UNBE("Unbekannt"),
    TEIL("Vertraut"),
    VOLL("Verifiziert");

    private static String prettyPrint;
    private final String  description;

    Vertrauensstufe(String description)
    {
        this.description = description;
    }

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (Vertrauensstufe vs : Vertrauensstufe.values()) {
                sb.append(vs.name()).append(" (").append(vs.getDescription()).append("), ");
            }
            String temp = sb.toString();
            prettyPrint = temp.substring(0, temp.length() - 2);
        }
        return prettyPrint;
    }

    public String getDescription()
    {
        return description;
    }
}
