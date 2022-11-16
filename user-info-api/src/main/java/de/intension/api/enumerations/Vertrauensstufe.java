package de.intension.api.enumerations;

public enum Vertrauensstufe
{

    KEIN("Keine"),
    UNBE("Unbekannt"),
    TEIL("Vertraut"),
    VOLL("Verifiziert");

    private final String description;

    Vertrauensstufe(String description)
    {
        this.description = description;
    }
}
