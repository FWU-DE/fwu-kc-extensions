package de.intension.rest;

import java.util.Collections;
import java.util.List;

import com.jayway.jsonpath.JsonPath;

public class BaseMapper
    implements IValueMapper
{

    private final String jsonPath;

    public BaseMapper(String jsonPath)
    {
        this.jsonPath = jsonPath;
    }

    public String getJsonPath()
    {
        return jsonPath;
    }

    @Override
    public List<String> mapValue(Object document, String jsonPath)
    {
        String value = JsonPath.parse(document).read(jsonPath, String.class);
        return Collections.singletonList(value);
    }
}
