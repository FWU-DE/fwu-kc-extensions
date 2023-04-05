package de.intension.api;

import de.intension.api.enumerations.*;

public enum UserInfoAttribute
{

    HEIMATORGANISATION_ID(UserInfoAttributeNames.HEIMATORGANISATION_ID, UserInfoAttributeNames.HEIMATORGANISATION_ID, Boolean.TRUE, null,
            "ID der Heimatorganisation ('Alias' des IdentityProviders)"),
    HEIMATORGANISATION_NAME(UserInfoAttributeNames.HEIMATORGANISATION_NAME, UserInfoAttributeNames.HEIMATORGANISATION_NAME, Boolean.TRUE, null,
            "Name der Heimatorganisation ('Display Name' des Identity Providers)"),
    HEIMATORGANISATION_BUNDESLAND(UserInfoAttributeNames.HEIMATORGANISATION_BUNDESLAND, UserInfoAttributeNames.HEIMATORGANISATION_BUNDESLAND, Boolean.TRUE,
            null, "Bundesland im Format ISO 3166-2:DE"),
    PERSON_FAMILIENNAME(UserInfoAttributeNames.PERSON_FAMILIENNAME, UserInfoAttributeNames.PERSON_FAMILIENNAME, Boolean.FALSE, null,
            "Familienname (Fallback: 'lastName' aus den User Properties)"),
    PERSON_FAMILIENNAME_INITIALEN(UserInfoAttributeNames.PERSON_FAMILIENNAME_INITIALEN, UserInfoAttributeNames.PERSON_FAMILIENNAME_INITIALEN, Boolean.FALSE,
            null, "Initial oder Initialen des Familiennamens"),
    PERSON_VORNAME(UserInfoAttributeNames.PERSON_VORNAME, UserInfoAttributeNames.PERSON_VORNAME, Boolean.FALSE, null,
            "Vorname (Fallback: 'firstName' aus den User Properties)"),
    PERSON_VORNAME_INITIALEN(UserInfoAttributeNames.PERSON_VORNAME_INITIALEN, UserInfoAttributeNames.PERSON_VORNAME_INITIALEN, Boolean.FALSE, null,
            "Initial oder Initialen des Vornamens"),
    PERSON_AKRONYM(UserInfoAttributeNames.PERSON_AKRONYM, UserInfoAttributeNames.PERSON_AKRONYM, Boolean.FALSE, null,
            "Akronym in Kleinbuchstaben - Zusammengesetzt aus den ersten beiden Buchstaben von Vorname und Familienname"),
    PERSON_GEBURTSDATUM(UserInfoAttributeNames.PERSON_GEBURTSDATUM, UserInfoAttributeNames.PERSON_GEBURTSDATUM, Boolean.FALSE, null,
            "Geburtsdatum im Format ISO-8601 (YYYY-MM-DD)"),
    PERSON_GEBURTSORT(UserInfoAttributeNames.PERSON_GEBURTSORT, UserInfoAttributeNames.PERSON_GEBURTSORT, Boolean.FALSE, null,
            "Geburtsort im Form Stadt, Land. Wenn kein Land angegeben dann wird Deutschland angenommen."),
    PERSON_VOLLJAEHRIG(UserInfoAttributeNames.PERSON_VOLLJAEHRIG, UserInfoAttributeNames.PERSON_VOLLJAEHRIG, Boolean.FALSE, null,
            "Ist die Person volljährig. [Ja,Nein]"),
    PERSON_ALTER(UserInfoAttributeNames.PERSON_ALTER, UserInfoAttributeNames.PERSON_ALTER, Boolean.FALSE, null,
            "Alter - Wird aus dem Geburtsdatum errechnet (falls vorhanden)"),
    PERSON_GESCHLECHT(UserInfoAttributeNames.PERSON_GESCHLECHT, UserInfoAttributeNames.PERSON_GESCHLECHT, Boolean.FALSE, null,
            "Geschlecht - Werte: " + Geschlecht.prettyPrint()),
    PERSON_LOKALISIERUNG(UserInfoAttributeNames.PERSON_LOKALISIERUNG, UserInfoAttributeNames.PERSON_LOKALISIERUNG, Boolean.TRUE, "de-DE",
            "Lokalisierung im Format RFC 5646 <ISO-639-1>-<ISO-3166> (z.B. de-DE)"),
    PERSON_VERTRAUENSSTUFE(UserInfoAttributeNames.PERSON_VERTRAUENSSTUFE, UserInfoAttributeNames.PERSON_VERTRAUENSSTUFE, Boolean.TRUE, Vertrauensstufe.VOLL,
            "Vertrauensstufe (default 'VOLL'). Werte: " + Vertrauensstufe.prettyPrint()),
    PERSON_REFERRER(UserInfoAttributeNames.PERSON_REFERRER, UserInfoAttributeNames.PERSON_REFERRER, Boolean.FALSE, null,
            "ID der Gruppe im Quellsystem. Wird vom Quellsystem vergeben und muss im Quellsystem eindeutig sein."),
    PERSON_KONTEXT_ID(UserInfoAttributeNames.PERSON_KONTEXT_ID, UserInfoAttributeNames.PERSON_KONTEXT_ID, Boolean.TRUE, null,
            "ID des Personenkontexts, welchem die Gruppe zugeordnet ist "),
    PERSON_KONTEXT_REFERRER(UserInfoAttributeNames.PERSON_KONTEXT_REFERRER, UserInfoAttributeNames.PERSON_KONTEXT_REFERRER, Boolean.TRUE, null,
            "ID der Gruppe im Quellsystem. Wird vom Quellsystem vergeben und muss im Quellsystem eindeutig sein."),
    PERSON_KONTEXT_ORG_VIDIS_ID(UserInfoAttributeNames.PERSON_KONTEXT_ORG_VIDIS_ID, UserInfoAttributeNames.PERSON_KONTEXT_ORG_VIDIS_ID, Boolean.TRUE, null,
            "Vidis Schulidentifikator"),
    PERSON_KONTEXT_ORG_ID(UserInfoAttributeNames.PERSON_KONTEXT_ORG_ID, UserInfoAttributeNames.PERSON_KONTEXT_ORG_ID, Boolean.TRUE, null,
            "Die „Identifikation einer „Organisation"),
    PERSON_KONTEXT_ORG_KENNUNG(UserInfoAttributeNames.PERSON_KONTEXT_ORG_KENNUNG, UserInfoAttributeNames.PERSON_KONTEXT_ORG_KENNUNG, Boolean.TRUE, null,
            "Die optionale Kennung (externe Identifikations-ID) einer „Organisation” muss innerhalb eines Organisationstyps eindeutig sein. Der Wert ist eine Kennung der Organisation, die von einem externen Verantwortlichen vergeben und kontrolliert wird. Beispielhaft ist für Organisationen vom Typ „Schule” die offizielle Schulnummer."),
    PERSON_KONTEXT_ORG_NAME(UserInfoAttributeNames.PERSON_KONTEXT_ORG_NAME, UserInfoAttributeNames.PERSON_KONTEXT_ORG_NAME, Boolean.TRUE, null,
            "Offizieller Name einer Organisation"),
    PERSON_KONTEXT_ORG_TYP(UserInfoAttributeNames.PERSON_KONTEXT_ORG_TYP, UserInfoAttributeNames.PERSON_KONTEXT_ORG_TYP, Boolean.TRUE, OrganisationsTyp.SCHULE,
            "Typ der Organisation. Werte: " + OrganisationsTyp.prettyPrint()),
    PERSON_KONTEXT_ROLLE(UserInfoAttributeNames.PERSON_KONTEXT_ROLLE, UserInfoAttributeNames.PERSON_KONTEXT_ROLLE, Boolean.FALSE, null,
            "Rolle der Person innerhalb der Organisation. Werte: " + Rolle.prettyPrint()),
    PERSON_KONTEXT_STATUS(UserInfoAttributeNames.PERSON_KONTEXT_STATUS, UserInfoAttributeNames.PERSON_KONTEXT_STATUS, Boolean.TRUE, PersonenStatus.AKTIV,
            "Status, den eine Person in einer Organisation in Bezug auf eine bestimmte Rolle hat. Werte: " + PersonenStatus.prettyPrint()),
    PERSON_KONTEXT_GRUPPEN(UserInfoAttributeNames.PERSON_KONTEXT_GRUPPEN, UserInfoAttributeNames.PERSON_KONTEXT_GRUPPEN, Boolean.TRUE, null,
            "Gruppen im Personenkontext, denen die Person zugeordnet ist."),
    PERSON_KONTEXT_LOESCHUNG(UserInfoAttributeNames.PERSON_KONTEXT_LOESCHUNG, UserInfoAttributeNames.PERSON_KONTEXT_LOESCHUNG, Boolean.TRUE, null,
            "Datum zu dem dieser Personenkontext nicht mehr gültig ist und gelöscht wird."),
    PERSON_KONTEXT_ARRAY_ID(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ID, UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ID, Boolean.TRUE, null,
            null),
    PERSON_KONTEXT_ARRAY_REFERRER(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_REFERRER, UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_REFERRER, Boolean.TRUE,
            null,
            null),
    PERSON_KONTEXT_ARRAY_ORG_ID(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_ID, UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_ID, Boolean.TRUE,
            null, null),
    PERSON_KONTEXT_ARRAY_ORG_VIDIS_ID(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_VIDIS_ID, UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_VIDIS_ID,
            Boolean.TRUE,
            null, null),
    PERSON_KONTEXT_ARRAY_ORG_KENNUNG(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_KENNUNG, UserInfoAttributeNames.PERSON_KONTEXT_ORG_KENNUNG, Boolean.TRUE,
            null, null),
    PERSON_KONTEXT_ARRAY_ORG_NAME(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_NAME, UserInfoAttributeNames.PERSON_KONTEXT_ORG_NAME, Boolean.TRUE, null,
            null),
    PERSON_KONTEXT_ARRAY_ORG_TYP(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_TYP, UserInfoAttributeNames.PERSON_KONTEXT_ORG_TYP, Boolean.TRUE,
            OrganisationsTyp.SCHULE, null),

    PERSON_KONTEXT_ARRAY_ROLLE(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ROLLE, UserInfoAttributeNames.PERSON_KONTEXT_ROLLE, Boolean.FALSE, null, null),
    PERSON_KONTEXT_ARRAY_STATUS(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_STATUS, UserInfoAttributeNames.PERSON_KONTEXT_STATUS, Boolean.TRUE,
            PersonenStatus.AKTIV, null),
    PERSON_KONTEXT_ARRAY_GRUPPEN(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_GRUPPEN, UserInfoAttributeNames.PERSON_KONTEXT_GRUPPEN, Boolean.TRUE, null, null),
    PERSON_KONTEXT_ARRAY_LOESCHUNG(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_LOESCHUNG, UserInfoAttributeNames.PERSON_KONTEXT_LOESCHUNG, Boolean.TRUE, null,
            null);

    private final String  attributeName;
    private final String  label;
    private final Object  defaultValue;
    private final Boolean enabled;

    private final String  helpText;

    UserInfoAttribute(String attributeName, String label, Boolean enabled, Object defaultValue, String helpText)
    {
        this.attributeName = attributeName;
        this.label = label;
        this.enabled = enabled;
        this.defaultValue = defaultValue;
        this.helpText = helpText;
    }

    public String getAttributeName()
    {
        return attributeName;
    }

    public String getLabel()
    {
        return label;
    }

    public Boolean isEnabled()
    {
        return enabled;
    }

    public Object getDefaultValue()
    {
        return defaultValue;
    }

    public String getHelpText()
    {
        return helpText;
    }
}
