package de.intension.api.enumerations;

public enum Jahrgangsstufe
{

    JS_01("01", "Jahrgangsstufe 1"),
    JS_02("02", "Jahrgangsstufe 2"),
    JS_03("03", "Jahrgangsstufe 3"),
    JS_04("04", "Jahrgangsstufe 4"),
    JS_05("05", "Jahrgangsstufe 5"),
    JS_06("06", "Jahrgangsstufe 6"),
    JS_07("07", "Jahrgangsstufe 7"),
    JS_08("08", "Jahrgangsstufe 8"),
    JS_09("09", "Jahrgangsstufe 9"),
    JS_10("10", "Jahrgangsstufe 10");

    private static String prettyPrint;
    private String        code;
    private String        description;

    Jahrgangsstufe(String code, String description)
    {
        this.code = code;
        this.description = description;
    }

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (Jahrgangsstufe jahrgangsstufe : Jahrgangsstufe.values()) {
                sb.append(jahrgangsstufe.name()).append(" (").append(jahrgangsstufe.getCode()).append(", ")
                    .append(jahrgangsstufe.getDescription())
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

    public String getDescription()
    {
        return description;
    }
}
