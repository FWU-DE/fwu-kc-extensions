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
public class PersonName
{

    @JsonProperty(UserInfoAttributeNames.FAMILIENNAME)
    private String familienname;
    @JsonProperty(UserInfoAttributeNames.VORNAME)
    private String vorname;
    @JsonProperty(UserInfoAttributeNames.AKRONYM)
    private String akronym;

    public String getAkronym()
    {
        if (akronym == null || akronym.isBlank()) {
            if (vorname != null && vorname.length() >= 2 && familienname != null && familienname.length() >= 2) {
                return vorname.substring(0, 2).concat(familienname.substring(0, 2));
            }
        }
        return akronym;
    }

    @JsonIgnore
    public boolean isEmpty()
    {
        return familienname == null && vorname == null && akronym == null;
    }

}
