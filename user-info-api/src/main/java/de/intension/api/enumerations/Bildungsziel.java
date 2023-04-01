package de.intension.api.enumerations;

public enum Bildungsziel
{

    GS("GS", "Grundschule"),
    HS("HS", "Hauptschule"),
    RS("RS", "Realschule"),
    GY_SEK_I("GY-SEK-I", "Gymnasium Sekundarstufe I");

    private static String prettyPrint;
    private String        code;
    private String        description;

    Bildungsziel(String code, String description)
    {
        this.code = code;
        this.description = description;
    }

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (Bildungsziel bildungsziel : Bildungsziel.values()) {
                sb.append(bildungsziel.name()).append(" (").append(bildungsziel.getCode()).append(", ")
                    .append(bildungsziel.getDescripion())
                    .append("), ");
            }
            String temp = sb.toString();
            prettyPrint = temp.substring(0, temp.length() - 2);
        }
        return prettyPrint;
    }

    public String getCode()
    {
        return code;
    }

    public String getDescripion()
    {
        return description;
    }
}
