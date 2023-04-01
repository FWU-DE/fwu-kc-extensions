package de.intension.api.enumerations;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GruppenDifferenzierung
{

    G("G", "G-Kurs"),
    E("E", "E-Kurs"),
    Z("Z", "Z-Kurs"),
    G_A("gA", "grundlegendes Anforderungsniveau"),
    E_A("eA", "erhöhtes Anforderungsniveau");

    private static String prettyPrint;
    private String        code;
    private String        description;

    GruppenDifferenzierung(String code, String description)
    {
        this.code = code;
        this.description = description;
    }

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (GruppenDifferenzierung gruppenDifferenzierung : GruppenDifferenzierung.values()) {
                sb.append(gruppenDifferenzierung.name()).append(" (").append(gruppenDifferenzierung.getCode())
                    .append(", ").append(gruppenDifferenzierung.getDescription())
                    .append("), ");
            }
            String temp = sb.toString();
            prettyPrint = temp.substring(0, temp.length() - 2);
        }
        return prettyPrint;
    }

    @JsonValue
    public String getCode()
    {
        return code;
    }

    public String getDescription()
    {
        return description;
    }
}
