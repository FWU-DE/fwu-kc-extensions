package de.intension.api.json;

import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.intension.api.UserInfoAttributeNames;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Geburt
{

    @JsonProperty(UserInfoAttributeNames.GEBURT_DATUM)
    private String  datum;

    @JsonProperty(UserInfoAttributeNames.GEBURT_ALTER)
    private Integer alter;

    @JsonIgnore
    public boolean isEmpty()
    {
        return StringUtil.isBlank(datum) && alter == null;
    }
}
