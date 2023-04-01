package de.intension.api.enumerations;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GruppenOption
{

    BILINGUAL("01"),
    HERKUNFTS_SPRACHLICH("02");

    private static String prettyPrint;
    private String        code;

    GruppenOption(String code)
    {
        this.code = code;
    }

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (GruppenOption optionen : GruppenOption.values()) {
                sb.append(optionen.name()).append(" (").append(optionen.getCode()).append("), ");
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
}
