package de.intension.api.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intension.api.UserInfoAttributeNames;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfo
{

    public static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonProperty("version")
    private String                   version      = "1.0.0";
    @JsonProperty(UserInfoAttributeNames.PID)
    private String                   pid;
    @JsonProperty(UserInfoAttributeNames.HEIMATORGANISATION)
    private HeimatOrganisation       heimatOrganisation;
    @JsonProperty(UserInfoAttributeNames.PERSON)
    private Person                   person;
    @JsonProperty(UserInfoAttributeNames.PERSONENKONTEXTE)
    private List<Personenkontext>    personenKontexte;

    @JsonIgnore
    public static String getJsonRepresentation(UserInfo userInfo)
        throws JsonProcessingException
    {
        return objectMapper.writeValueAsString(userInfo);
    }

    @JsonIgnore
    public boolean isEmpty()
    {
        return pid == null || (heimatOrganisation == null || heimatOrganisation.isEmpty()) && (person == null || person.isEmpty())
                && (personenKontexte == null || personenKontexte.isEmpty());
    }

    public List<Personenkontext> getPersonenKontexte()
    {
        if (personenKontexte == null) {
            personenKontexte = new ArrayList<>();
        }
        return personenKontexte;
    }

}
