package de.intension.rest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONObject;

public class GruppenMapper
    implements IValueMapper
{

    private final String jsonPath;

    public GruppenMapper(String jsonPath)
    {
        this.jsonPath = jsonPath;
    }

    @Override
    public String getJsonPath()
    {
        return jsonPath;
    }

    @Override
    public List<String> mapValue(Object document, String jsonPath)
    {
        Integer count = JsonPath.read(document, jsonPath + ".length()");
        List<String> gruppen = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String gruppenPath = String.format("%s[%d]", jsonPath, i);
            LinkedHashMap<String, String> jsonMap = JsonPath.read(document, gruppenPath);
            gruppen.add(new JSONObject(jsonMap).toJSONString());
        }
        return gruppen;
    }
}
