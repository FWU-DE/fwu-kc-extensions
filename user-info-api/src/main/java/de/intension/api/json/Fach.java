package de.intension.api.json;

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
public class Fach
{

    /**
     * Since spec says that different Bundesland could have different Code-List we are just using code
     * as we received it.
     */
    @JsonProperty(UserInfoAttributeNames.GRUPPE_FACH_CODE)
    private String code;
}
