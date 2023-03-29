package de.intension.mapper.user;

import de.intension.api.enumerations.GermanBoolean;

public class UserVolljaehrigkeitHelper
{

    private static final Integer VOLLJAEHRIGKEIT = 18;

    public GermanBoolean isVolljaehrig(Integer age)
    {
        return GermanBoolean.fromBoolean(age >= VOLLJAEHRIGKEIT);
    }
}