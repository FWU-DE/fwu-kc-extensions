package de.intension.api;

import de.intension.api.enumerations.Geschlecht;
import de.intension.api.enumerations.OrganisationsTyp;
import de.intension.api.enumerations.PersonenStatus;
import de.intension.api.enumerations.Vertrauensstufe;

public enum UserInfoAttribute
{

    HEIMATORGANISATION_NAME(UserInfoAttributeNames.HEIMATORGANISATION_NAME, UserInfoAttributeNames.HEIMATORGANISATION_NAME, Boolean.TRUE, null),
    HEIMATORGANISATION_BUNDESLAND(UserInfoAttributeNames.HEIMATORGANISATION_BUNDESLAND, UserInfoAttributeNames.HEIMATORGANISATION_BUNDESLAND, Boolean.TRUE,
            null),
    PERSON_FAMILIENNAME(UserInfoAttributeNames.PERSON_FAMILIENNAME, UserInfoAttributeNames.PERSON_FAMILIENNAME, Boolean.FALSE, null),
    PERSON_VORNAME(UserInfoAttributeNames.PERSON_VORNAME, UserInfoAttributeNames.PERSON_VORNAME, Boolean.FALSE, null),
    PERSON_AKRONYM(UserInfoAttributeNames.PERSON_AKRONYM, UserInfoAttributeNames.PERSON_AKRONYM, Boolean.FALSE, null),
    PERSON_GEBURTSDATUM(UserInfoAttributeNames.PERSON_GEBURTSDATUM, UserInfoAttributeNames.PERSON_GEBURTSDATUM, Boolean.FALSE, null),
    PERSON_GESCHLECHT(UserInfoAttributeNames.PERSON_GESCHLECHT, UserInfoAttributeNames.PERSON_GESCHLECHT, Boolean.FALSE, Geschlecht.X),
    PERSON_LOKALISIERUNG(UserInfoAttributeNames.PERSON_LOKALISIERUNG, UserInfoAttributeNames.PERSON_LOKALISIERUNG, Boolean.TRUE, "de-DE"),
    PERSON_VERTRAUENSSTUFE(UserInfoAttributeNames.PERSON_VERTRAUENSSTUFE, UserInfoAttributeNames.PERSON_VERTRAUENSSTUFE, Boolean.TRUE, Vertrauensstufe.VOLL),

    PERSON_KONTEXT_ID(UserInfoAttributeNames.PERSON_KONTEXT_ID, UserInfoAttributeNames.PERSON_KONTEXT_ID, Boolean.TRUE, Vertrauensstufe.VOLL),
    PERSON_KONTEXT_ORG_VIDIS_ID(UserInfoAttributeNames.PERSON_KONTEXT_ORG_VIDIS_ID, UserInfoAttributeNames.PERSON_KONTEXT_ORG_VIDIS_ID, Boolean.TRUE, null),
    PERSON_KONTEXT_ORG_ID(UserInfoAttributeNames.PERSON_KONTEXT_ORG_ID, UserInfoAttributeNames.PERSON_KONTEXT_ORG_ID, Boolean.TRUE, null),
    PERSON_KONTEXT_ORG_KENNUNG(UserInfoAttributeNames.PERSON_KONTEXT_ORG_KENNUNG, UserInfoAttributeNames.PERSON_KONTEXT_ORG_KENNUNG, Boolean.TRUE, null),
    PERSON_KONTEXT_ORG_NAME(UserInfoAttributeNames.PERSON_KONTEXT_ORG_NAME, UserInfoAttributeNames.PERSON_KONTEXT_ORG_NAME, Boolean.TRUE, null),
    PERSON_KONTEXT_ORG_TYP(UserInfoAttributeNames.PERSON_KONTEXT_ORG_TYP, UserInfoAttributeNames.PERSON_KONTEXT_ORG_TYP, Boolean.TRUE, OrganisationsTyp.SCHULE),
    PERSON_KONTEXT_ROLLE(UserInfoAttributeNames.PERSON_KONTEXT_ROLLE, UserInfoAttributeNames.PERSON_KONTEXT_ROLLE, Boolean.FALSE, null),
    PERSON_KONTEXT_STATUS(UserInfoAttributeNames.PERSON_KONTEXT_STATUS, UserInfoAttributeNames.PERSON_KONTEXT_STATUS, Boolean.TRUE, PersonenStatus.AKTIV),
    //person context array
    PERSON_KONTEXT_ARRAY_ID(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ID, UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ID, Boolean.TRUE,
            null),
    PERSON_KONTEXT_ARRAY_ORG_ID(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_ID, UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_ID, Boolean.TRUE,
            null),
    PERSON_KONTEXT_ARRAY_ORG_VIDIS_ID(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_VIDIS_ID, UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_VIDIS_ID,
            Boolean.TRUE,
            null),
    PERSON_KONTEXT_ARRAY_ORG_KENNUNG(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_KENNUNG, UserInfoAttributeNames.PERSON_KONTEXT_ORG_KENNUNG, Boolean.TRUE,
            null),
    PERSON_KONTEXT_ARRAY_ORG_NAME(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_NAME, UserInfoAttributeNames.PERSON_KONTEXT_ORG_NAME, Boolean.TRUE, null),
    PERSON_KONTEXT_ARRAY_ORG_TYP(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ORG_TYP, UserInfoAttributeNames.PERSON_KONTEXT_ORG_TYP, Boolean.TRUE,
            OrganisationsTyp.SCHULE),
    PERSON_KONTEXT_ARRAY_ROLLE(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_ROLLE, UserInfoAttributeNames.PERSON_KONTEXT_ROLLE, Boolean.FALSE, null),
    PERSON_KONTEXT_ARRAY_STATUS(UserInfoAttributeNames.PERSON_KONTEXT_ARRAY_STATUS, UserInfoAttributeNames.PERSON_KONTEXT_STATUS, Boolean.TRUE,
            PersonenStatus.AKTIV);

    private final String  attributeName;
    private final String  label;
    private final Object  defaultValue;
    private final Boolean enabled;

    UserInfoAttribute(String attributeName, String label, Boolean enabled, Object defaultValue)
    {
        this.attributeName = attributeName;
        this.label = label;
        this.enabled = enabled;
        this.defaultValue = defaultValue;
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
}
