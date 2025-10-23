package de.intension.api.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import de.intension.api.UserInfoAttributeNames;
import de.intension.util.JsonSerialization;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GruppeWithZugehoerigkeit {

    @JsonProperty(UserInfoAttributeNames.GRUPPE)
    private Gruppe gruppe;
    @JsonProperty(UserInfoAttributeNames.GRUPPEN_ZUGEHOERIGKEIT)
    private GruppenZugehoerigkeit gruppenZugehoerigkeit;

    @JsonIgnore
    public String getJsonRepresentation()
            throws JsonProcessingException {
        return JsonSerialization.writeValueAsString(this);
    }
}
