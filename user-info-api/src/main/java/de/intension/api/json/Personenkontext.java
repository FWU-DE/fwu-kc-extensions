package de.intension.api.json;

import java.util.List;

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

    @JsonProperty(UserInfoAttributeNames.ID)
    private String                         id;
    @JsonProperty(UserInfoAttributeNames.ORGANISATION)
    private Organisation                   organisation;
    @JsonProperty(UserInfoAttributeNames.ROLLE)
    private Rolle                          rolle;
    @JsonProperty(UserInfoAttributeNames.PERSONENSTATUS)
    private PersonenStatus                 personenstatus;
    @JsonProperty(UserInfoAttributeNames.REFERRER)
    private String                         referrer;
    @JsonProperty(UserInfoAttributeNames.GRUPPEN)
    private List<GruppeWithZugehoerigkeit> gruppen;
    @JsonProperty(UserInfoAttributeNames.LOESCHUNG)
    private Loeschung                      loeschung;

    @JsonIgnore
    public boolean isEmpty()
    {
        return id == null || (organisation == null || organisation.isEmpty()) && rolle == null && (referrer == null || referrer.isEmpty()) && loeschung == null
                && (gruppen == null || gruppen.isEmpty());
    }

}
