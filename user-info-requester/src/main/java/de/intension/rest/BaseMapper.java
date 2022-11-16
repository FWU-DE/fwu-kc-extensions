package de.intension.rest;

public class BaseMapper
    implements IValueMapper{

    private String jsonPath;

    public BaseMapper(String jsonPath){
        this.jsonPath = jsonPath;
    }

    public String getJsonPath(){
        return jsonPath;
    }

    @Override public String mapValue(String value)
    {
        return value;
    }
}
