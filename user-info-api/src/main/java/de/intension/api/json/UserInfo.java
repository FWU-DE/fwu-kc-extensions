package de.intension.api.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.models.ProtocolMapperModel;

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

    public static final ObjectMapper objectMapper      = new ObjectMapper();
    private static final String      VERSION_ATTRIBUTE = "version";
    @JsonProperty(VERSION_ATTRIBUTE)
    private String                   version           = "1.0.0";
    @JsonProperty(UserInfoAttributeNames.PID)
    private String                   pid;
    @JsonProperty(UserInfoAttributeNames.HEIMATORGANISATION)
    private HeimatOrganisation       heimatOrganisation;
    @JsonProperty(UserInfoAttributeNames.PERSON)
    private Person                   person;
    @JsonProperty(UserInfoAttributeNames.PERSONENKONTEXTE)
    private List<Personenkontext>    personenKontexte;

    @JsonIgnore
    public String getJsonRepresentation()
        throws JsonProcessingException
    {
        return objectMapper.writeValueAsString(this);
    }

    @JsonIgnore
    public Map<ProtocolMapperModel, String> getChildJsonRepresentations(boolean accessToken, boolean idToken, boolean userInfo)
        throws JsonProcessingException
    {
        HashMap<ProtocolMapperModel, String> representations = new HashMap<>();
        representations.put(getDynamicMapperModel(VERSION_ATTRIBUTE, "String", accessToken, idToken, userInfo), getVersion());
        representations.put(getDynamicMapperModel(UserInfoAttributeNames.PID, "String", accessToken, idToken, userInfo), getPid());
        if (heimatOrganisation != null && !heimatOrganisation.isEmpty()) {
            representations.put(getDynamicMapperModel(UserInfoAttributeNames.HEIMATORGANISATION, "JSON", accessToken, idToken, userInfo),
                                objectMapper.writeValueAsString(getHeimatOrganisation()));
        }
        if (person != null && !person.isEmpty()) {
            representations.put(getDynamicMapperModel(UserInfoAttributeNames.PERSON, "JSON", accessToken, idToken, userInfo),
                                objectMapper.writeValueAsString(getPerson()));
        }
        if (personenKontexte != null && !personenKontexte.isEmpty()) {
            representations.put(getDynamicMapperModel(UserInfoAttributeNames.PERSONENKONTEXTE, "JSON", accessToken, idToken, userInfo),
                                objectMapper.writeValueAsString(getPersonenKontexte()));
        }
        return representations;
    }

    @JsonIgnore
    private ProtocolMapperModel getDynamicMapperModel(String claimName, String claimType, boolean accessToken, boolean idToken, boolean userInfo)
    {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setId(claimName + "_dynamic");
        mapper.setProtocol("openid-connect");
        Map<String, String> config = new HashMap<>();
        config.put("claim.name", claimName);
        config.put("jsonType.label", claimType);
        if (accessToken) {
            config.put("access.token.claim", "true");
        }
        if (idToken) {
            config.put("id.token.claim", "true");
        }
        if (userInfo) {
            config.put("userinfo.token.claim", "true");
        }
        mapper.setConfig(config);
        return mapper;
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
