package de.intension.rest;

import java.util.List;

public interface IValueMapper
{

    String getJsonPath();

    List<String> mapValue(Object document, String jsonPath);
}
