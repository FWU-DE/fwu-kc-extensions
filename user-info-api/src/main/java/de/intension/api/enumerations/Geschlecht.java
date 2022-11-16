package de.intension.api.enumerations;

public enum Geschlecht
{

    M("männlich"),
    W("weiblich"),
    D("diverse"),
    X("Keine Angaben");

    String description;

    Geschlecht(String description)
    {
        this.description = description;
    }
}
