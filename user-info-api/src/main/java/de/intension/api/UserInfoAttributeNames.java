package de.intension.api;

public class UserInfoAttributeNames
{

    public static final String    PID                               = "pid";
    public static final String    NAME                              = "name";
    public static final String    BUNDESLAND                        = "bundesland";
    public static final String    HEIMATORGANISATION                = "heimatorganisation";
    public static final String    PERSON                            = "person";
    public static final String    FAMILIENNAME                      = "familienname";
    public static final String    FAMILIENNAME_INITIALEN            = "initialenFamilienname";
    public static final String    VORNAME                           = "vorname";
    public static final String    VORNAME_INITIALEN                 = "initialenVorname";
    public static final String    AKRONYM                           = "akronym";
    public static final String    GESCHLECHT                        = "geschlecht";
    public static final String    LOKALISIERUNG                     = "lokalisierung";
    public static final String    VERTRAUENSSTUFE                   = "vertrauensstufe";
    public static final String    GEBURT                            = "geburt";
    public static final String    GEBURT_DATUM                      = "datum";
    public static final String    GEBURT_ALTER                      = "alter";
    public static final String    GEBURT_VOLLJAEHRIG                = "volljaehrig";
    public static final String    GEBURT_ORT                        = "geburtsort";
    public static final String    PERSONENKONTEXTE                  = "personenkontexte";
    public static final String    ROLLE                             = "rolle";
    public static final String    PERSONENSTATUS                    = "personenstatus";
    public static final String    ORGANISATION                      = "organisation";
    public static final String    ORG_ID                            = "orgid";
    public static final String    ORG_KENNUNG                       = "kennung";
    public static final String    ORG_TYP                           = "typ";
    public static final String    VIDIS_SCHULIDENTIFIKATOR          = "vidis_schulidentifikator";
    public static final String    ID                                = "id";
    public static final String    GRUPPEN                           = "gruppen";
    public static final String    GRUPPE                            = "gruppe";
    public static final String    REFERRER                          = "referrer";
    public static final String    GRUPPE_MANDANT                    = "mandant";
    public static final String    GRUPPE_BEZEICHNUNG                = "bezeichnung";
    public static final String    GRUPPE_BESCHREIBUNG               = "beschreibung";
    public static final String    GRUPPE_THEMA                      = "thema";
    public static final String    GRUPPE_TYP                        = "typ";
    public static final String    GRUPPE_BEREICH                    = "bereich";
    public static final String    GRUPPE_OPTIONEN                   = "optionen";
    public static final String    GRUPPE_DIFFERENZIERUNG            = "differenzierung";
    public static final String    GRUPPE_BILDUNGSZIELE              = "bildungsziele";
    public static final String    GRUPPE_JAHRGANGSSTUFEN            = "jahrgangsstufen";
    public static final String    GRUPPE_FAECHER                    = "faecher";
    public static final String    GRUPPE_FACH_CODE                  = "code";
    public static final String    GRUPPE_REFERENZ_GRUPPEN           = "referenzgruppen";
    public static final String    GRUPPE_SICHT_FREIGABE             = "sichtfreigabe";
    public static final String    LAUFZEIT                          = "laufzeit";
    public static final String    LAUFZEIT_VON                      = "von";
    public static final String    LAUFZEIT_VON_LERN_PERIODE         = "vonlernperiode";
    public static final String    LAUFZEIT_BIS                      = "bis";
    public static final String    LAUFZEIT_BIS_LERN_PERIODE         = "bislernperiode";
    public static final String    ROLLEN                            = "rollen";
    public static final String    REVISION                          = "revision";
    public static final String    GRUPPEN_ZUGEHOERIGKEIT            = "gruppenzugehoerigkeit";
    public static final String    LOESCHUNG                         = "loeschung";
    public static final String    ZEITPUNKT                         = "zeitpunkt";
    private static final String   CONCAT                            = "%s.%s";
    protected static final String PERSON_GEBURTSDATUM               = String.format("%s.geburtsdatum", PERSON);
    protected static final String PERSON_GEBURTSORT                 = String.format(CONCAT, PERSON, GEBURT_ORT);
    protected static final String PERSON_VOLLJAEHRIG                = String.format(CONCAT, PERSON, GEBURT_VOLLJAEHRIG);
    public static final String    PERSON_REFERRER                   = String.format(CONCAT, PERSON, REFERRER);
    protected static final String PERSON_KONTEXT                    = String.format("%s.kontext", PERSON);
    protected static final String PERSON_KONTEXT_REFERRER           = String.format(CONCAT, PERSON_KONTEXT, REFERRER);
    protected static final String PERSON_KONTEXT_ORG                = String.format("%s.org", PERSON_KONTEXT);
    protected static final String PERSON_KONTEXT_GRUPPEN            = String.format(CONCAT, PERSON_KONTEXT, GRUPPEN);
    protected static final String PERSON_KONTEXT_LOESCHUNG          = String.format(CONCAT, PERSON_KONTEXT, LOESCHUNG);
    protected static final String PERSON_KONTEXT_STATUS             = String.format("%s.status", PERSON_KONTEXT);
    protected static final String PERSON_KONTEXT_ARRAY              = String.format("%s.kontext[#]", PERSON);
    protected static final String PERSON_KONTEXT_ARRAY_ORG          = String.format("%s.org", PERSON_KONTEXT_ARRAY);
    protected static final String PERSON_KONTEXT_ARRAY_STATUS       = String.format("%s.status", PERSON_KONTEXT_ARRAY);
    protected static final String PERSON_KONTEXT_ARRAY_GRUPPEN      = String.format(CONCAT, PERSON_KONTEXT_ARRAY, GRUPPEN);
    protected static final String PERSON_KONTEXT_ARRAY_LOESCHUNG    = String.format(CONCAT, PERSON_KONTEXT_ARRAY, LOESCHUNG);
    protected static final String PERSON_KONTEXT_ARRAY_REFERRER     = String.format(CONCAT, PERSON_KONTEXT_ARRAY, REFERRER);
    protected static final String PERSON_ALTER                      = String.format(CONCAT, PERSON, GEBURT_ALTER);
    protected static final String PERSON_KONTEXT_ID                 = String.format(CONCAT, PERSON_KONTEXT, ID);
    protected static final String PERSON_KONTEXT_ORG_ID             = String.format(CONCAT, PERSON_KONTEXT_ORG, ID);
    protected static final String PERSON_KONTEXT_ARRAY_ID           = String.format(CONCAT, PERSON_KONTEXT_ARRAY, ID);
    protected static final String PERSON_KONTEXT_ARRAY_ORG_ID       = String.format(CONCAT, PERSON_KONTEXT_ARRAY_ORG, ID);

    protected static final String HEIMATORGANISATION_ID             = String.format(CONCAT, HEIMATORGANISATION, ID);
    protected static final String HEIMATORGANISATION_NAME           = String.format(CONCAT, HEIMATORGANISATION, NAME);
    protected static final String HEIMATORGANISATION_BUNDESLAND     = String.format(CONCAT, HEIMATORGANISATION, BUNDESLAND);
    protected static final String PERSON_FAMILIENNAME               = String.format(CONCAT, PERSON, FAMILIENNAME);
    protected static final String PERSON_FAMILIENNAME_INITIALEN     = String.format(CONCAT, PERSON, FAMILIENNAME_INITIALEN);
    protected static final String PERSON_VORNAME                    = String.format(CONCAT, PERSON, VORNAME);
    protected static final String PERSON_VORNAME_INITIALEN          = String.format(CONCAT, PERSON, VORNAME_INITIALEN);
    protected static final String PERSON_AKRONYM                    = String.format(CONCAT, PERSON, AKRONYM);
    protected static final String PERSON_GESCHLECHT                 = String.format(CONCAT, PERSON, GESCHLECHT);
    protected static final String PERSON_LOKALISIERUNG              = String.format(CONCAT, PERSON, LOKALISIERUNG);
    protected static final String PERSON_VERTRAUENSSTUFE            = String.format(CONCAT, PERSON, VERTRAUENSSTUFE);
    protected static final String PERSON_KONTEXT_ORG_VIDIS_ID       = String.format(CONCAT, PERSON_KONTEXT_ORG, VIDIS_SCHULIDENTIFIKATOR);
    protected static final String PERSON_KONTEXT_ORG_KENNUNG        = String.format(CONCAT, PERSON_KONTEXT_ORG, ORG_KENNUNG);
    protected static final String PERSON_KONTEXT_ORG_NAME           = String.format(CONCAT, PERSON_KONTEXT_ORG, NAME);
    protected static final String PERSON_KONTEXT_ORG_TYP            = String.format(CONCAT, PERSON_KONTEXT_ORG, ORG_TYP);
    protected static final String PERSON_KONTEXT_ROLLE              = String.format(CONCAT, PERSON_KONTEXT, ROLLE);
    protected static final String PERSON_KONTEXT_ARRAY_ORG_VIDIS_ID = String.format(CONCAT, PERSON_KONTEXT_ARRAY_ORG, VIDIS_SCHULIDENTIFIKATOR);
    protected static final String PERSON_KONTEXT_ARRAY_ORG_KENNUNG  = String.format(CONCAT, PERSON_KONTEXT_ARRAY_ORG, ORG_KENNUNG);
    protected static final String PERSON_KONTEXT_ARRAY_ORG_NAME     = String.format(CONCAT, PERSON_KONTEXT_ARRAY_ORG, NAME);
    protected static final String PERSON_KONTEXT_ARRAY_ORG_TYP      = String.format(CONCAT, PERSON_KONTEXT_ARRAY_ORG, ORG_TYP);
    protected static final String PERSON_KONTEXT_ARRAY_ROLLE        = String.format(CONCAT, PERSON_KONTEXT_ARRAY, ROLLE);

    private UserInfoAttributeNames()
    {
    }

}
