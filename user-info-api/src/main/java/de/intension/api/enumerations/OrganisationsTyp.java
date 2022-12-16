package de.intension.api.enumerations;

public enum OrganisationsTyp
{

    SCHULE,
    ANBIETER,
    SONSTIGE,
    UNBEST;

    private static String prettyPrint;

    public static String prettyPrint()
    {
        if (prettyPrint == null) {
            StringBuilder sb = new StringBuilder();
            for (OrganisationsTyp orgTyp : OrganisationsTyp.values()) {
                sb.append(orgTyp.name()).append(", ");
            }
            String temp = sb.toString();
            prettyPrint = temp.substring(0, temp.length() - 2);
        }
        return prettyPrint;
    }
}
