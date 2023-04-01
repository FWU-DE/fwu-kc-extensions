package de.intension.rest.sanis;

import java.util.List;
import java.util.stream.Collectors;

import de.intension.rest.BaseMapper;

public class UpperCaseMapper extends BaseMapper
{

    public UpperCaseMapper(String jsonPath)
    {
        super(jsonPath);
    }

    @Override
    public List<String> mapValue(Object document, String jsonPath)
    {
        return super.mapValue(document, jsonPath).stream().map(String::toUpperCase).collect(Collectors.toList());
    }
}
