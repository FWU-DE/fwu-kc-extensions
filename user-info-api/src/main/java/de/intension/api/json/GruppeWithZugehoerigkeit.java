package de.intension.api.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.intension.api.UserInfoAttributeNames;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GruppeWithZugehoerigkeit
{

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @JsonProperty(UserInfoAttributeNames.GRUPPE)
    private Gruppe                gruppe;
    @JsonProperty(UserInfoAttributeNames.GRUPPEN_ZUGEHOERIGKEIT)
    private GruppenZugehoerigkeit gruppenZugehoerigkeit;

    @JsonIgnore
    public String getJsonRepresentation()
        throws JsonProcessingException
    {
        return objectMapper.writeValueAsString(this);
    }
}
