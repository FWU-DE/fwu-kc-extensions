package de.intension.api.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.intension.api.UserInfoAttributeNames;
import de.intension.api.enumerations.PersonenStatus;
import de.intension.api.enumerations.Rolle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Personenkontext
{

    @JsonProperty(UserInfoAttributeNames.KTID)
    private String         ktid;
    @JsonProperty(UserInfoAttributeNames.ORGANISATION)
    private Organisation   organisation;
    @JsonProperty(UserInfoAttributeNames.ROLLE)
    private Rolle          rolle;
    @JsonProperty(UserInfoAttributeNames.PERSONENSTATUS)
    private PersonenStatus personenstatus;

    @JsonIgnore
    public boolean isEmpty()
    {
        return ktid == null || (organisation == null || organisation.isEmpty()) && rolle == null;
    }

}
