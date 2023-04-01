package de.intension.api.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import de.intension.api.UserInfoAttributeNames;
import de.intension.api.enumerations.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Gruppe
{

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @JsonProperty(UserInfoAttributeNames.ID)
    private GruppenId              id;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_MANDANT)
    private String                 mandant;
    @JsonProperty(UserInfoAttributeNames.ORG_ID)
    private String                 orgid;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_REFERRER)
    private GruppenId              referrer;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_BEZEICHNUNG)
    private String                 bezeichnung;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_THEMA)
    private String                 thema;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_BESCHREIBUNG)
    private String                 beschreibung;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_TYP)
    private Gruppentyp             typ;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_BEREICH)
    private Gruppenbereich         bereich;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_OPTIONEN)
    private List<GruppenOption>    optionen;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_DIFFERENZIERUNG)
    private GruppenDifferenzierung differenzierung;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_BILDUNGSZIELE)
    private List<Bildungsziel>     bildungsziel;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_JAHRGANGSSTUFEN)
    private List<Jahrgangsstufe>   jahrgangstufen;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_FAECHER)
    private List<Fach>             faecher;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_REFERENZ_GRUPPEN)
    private List<ReferenzGruppe>   referenzGruppen;
    @JsonProperty(UserInfoAttributeNames.LAUFZEIT)
    private Laufzeit               laufzeit;
    @JsonProperty(UserInfoAttributeNames.REVISION)
    private String                 revision;

    @JsonIgnore
    public String getJsonRepresentation()
        throws JsonProcessingException
    {
        return objectMapper.writeValueAsString(this);
    }
}
