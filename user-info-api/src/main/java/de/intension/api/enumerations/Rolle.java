package de.intension.api.enumerations;

public enum Rolle
{

    LERN("Lernende/r"),
    LEHR("Lehrende/r"),
    EXTERN("Externe Person"),
    ORGADMIN("Organisationsadministrator"),
    LEIT("Organisationsleitung"),
    SYSADMIN("Systemadministrator");

    private static String prettyPrint;
    private final String  description;

    Rolle(String description)
    {
        this.description = description;
    }

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (Rolle rolle : Rolle.values()) {
                sb.append(rolle.name()).append(" (").append(rolle.getDescription()).append("), ");
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
