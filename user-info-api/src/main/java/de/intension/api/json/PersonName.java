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
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PersonName
{

    @JsonProperty(UserInfoAttributeNames.FAMILIENNAME)
    private String familienname;
    @JsonProperty(UserInfoAttributeNames.VORNAME)
    private String vorname;

    @JsonProperty(UserInfoAttributeNames.FAMILIENNAME_INITIALEN)
    private String initialenFamilienname;

    @JsonProperty(UserInfoAttributeNames.VORNAME_INITIALEN)
    private String initialenVorname;

    @JsonProperty(UserInfoAttributeNames.AKRONYM)
    private String akronym;

    @JsonIgnore
    public boolean isEmpty()
    {
        return StringUtil.isBlank(familienname) && StringUtil.isBlank(vorname) && StringUtil.isBlank(akronym) && StringUtil.isBlank(initialenFamilienname)
                && StringUtil.isBlank(initialenVorname);
    }

}
