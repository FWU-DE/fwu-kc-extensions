package de.intension.api.enumerations;

public enum Rolle
{

    LERN("Lernende/r"),
    LEHR("Lehrende/r"),
    EXTERN("Externe Person"),
    ORGADMIN("Organisationsadministrator"),
    LEIT("Organisationsleitung"),
    SYSADMIN("Systemadministrator");

    private final String description;

    Rolle(String description)
    {
        this.description = description;
    }

}
