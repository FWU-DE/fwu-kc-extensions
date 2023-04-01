package de.intension.api.enumerations;

public enum Gruppenbereich
{

    PFLICHT("Pflichtunterricht"),
    WAHL("Wahlunterricht"),
    WAHLPFLICHT("Wahlpflichtunterricht");

    private static String prettyPrint;
    private String        description;

    Gruppenbereich(String description)
    {
        this.description = description;
    }

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (Gruppenbereich gruppenbereich : Gruppenbereich.values()) {
                sb.append(gruppenbereich.name()).append(" (")
                    .append(gruppenbereich.getDescription())
                    .append("), ");
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
