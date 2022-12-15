package de.intension.api.enumerations;

public enum Geschlecht
{

    M("männlich"),
    W("weiblich"),
    D("diverse"),
    X("Keine Angaben");

    private static String prettyPrint;
    private final  String description;

    Geschlecht(String description)
    {
        this.description = description;
    }

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (Geschlecht geschlecht : Geschlecht.values()) {
                sb.append(geschlecht.name()).append(" (").append(geschlecht.getDescription()).append("), ");
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
