package de.intension.api.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.intension.api.UserInfoAttributeNames;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Geburt
{

    @JsonProperty(UserInfoAttributeNames.GEBURT_DATUM)
    private String datum;

    @JsonIgnore
    public boolean isEmpty()
    {
        return (datum == null || datum.isBlank());
    }
}
