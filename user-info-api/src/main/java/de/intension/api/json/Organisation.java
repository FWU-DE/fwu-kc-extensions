package de.intension.api.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.intension.api.UserInfoAttributeNames;
import de.intension.api.enumerations.OrganisationsTyp;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Organisation
{

    @JsonProperty(UserInfoAttributeNames.ORG_ID)
    private String           orgid;
    @JsonProperty(UserInfoAttributeNames.ORG_KENNUNG)
    private String           kennung;
    @JsonProperty(UserInfoAttributeNames.NAME)
    private String           name;
    @JsonProperty(UserInfoAttributeNames.ORG_TYP)
    private OrganisationsTyp typ;
    @JsonProperty(UserInfoAttributeNames.VIDIS_SCHULIDENTIFIKATOR)
    private String           vidisSchulidentifikator;

    @JsonIgnore
    public boolean isEmpty()
    {
        return (orgid == null || name == null && kennung == null && vidisSchulidentifikator == null);
    }
}
