package de.intension.api.enumerations;

import com.fasterxml.jackson.annotation.JsonIgnore;

public enum Gruppentyp
{

    KLASSE("Schulklasse"),
    KURS("Kurs/Unterricht"),
    SONSTIG("Sonstige Gruppe");

    private static String prettyPrint;
    private final String  description;

    Gruppentyp(String description)
    {
        this.description = description;
    }

    @JsonIgnore

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (Gruppentyp gruppentyp : Gruppentyp.values()) {
                sb.append(gruppentyp.name()).append(" (")
                    .append(gruppentyp.getDescription())
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
