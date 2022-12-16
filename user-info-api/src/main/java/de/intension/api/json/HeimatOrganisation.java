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
public class HeimatOrganisation
{

    @JsonProperty(UserInfoAttributeNames.ID)
    private String id;
    @JsonProperty(UserInfoAttributeNames.NAME)
    private String name;
    @JsonProperty(UserInfoAttributeNames.BUNDESLAND)
    private String bundesland;

    @JsonIgnore
    public boolean isEmpty()
    {
        return StringUtil.isBlank(id) && StringUtil.isBlank(name) && StringUtil.isBlank(bundesland);
    }

}
