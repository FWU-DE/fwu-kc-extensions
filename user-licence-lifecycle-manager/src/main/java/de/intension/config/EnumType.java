package de.intension.config;

public interface EnumType
{

    String name();

    int ordinal();

    default String asString()
    {
        return name();
    }
}