package de.intension.rest.sanis;

import de.intension.rest.BaseMapper;

public class UpperCaseMapper extends BaseMapper {

    public UpperCaseMapper(String jsonPath){
        super(jsonPath);
    }

    @Override public String mapValue(String value)
    {
        if(value != null){
            return value.toUpperCase();
        }
        return value;
    }
}
