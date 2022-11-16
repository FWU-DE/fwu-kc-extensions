package de.intension.api.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.intension.api.UserInfoAttributeNames;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HeimatOrganisation
{

    @JsonProperty(UserInfoAttributeNames.NAME)
    private String name;
    @JsonProperty(UserInfoAttributeNames.BUNDESLAND)
    private String bundesland;

    @JsonIgnore
    public boolean isEmpty()
    {
        return name == null && bundesland == null;
    }

}
