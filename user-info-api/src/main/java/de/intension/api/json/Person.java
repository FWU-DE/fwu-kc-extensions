package de.intension.api.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.intension.api.UserInfoAttributeNames;
import de.intension.api.enumerations.Geschlecht;
import de.intension.api.enumerations.Vertrauensstufe;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Person
{

    @JsonProperty(UserInfoAttributeNames.NAME)
    private PersonName personName;
    @JsonProperty(UserInfoAttributeNames.GEBURT)
    private Geburt          geburt;
    @JsonProperty(UserInfoAttributeNames.GESCHLECHT)
    private Geschlecht      geschlecht;
    @JsonProperty(UserInfoAttributeNames.LOKALISIERUNG)
    private String          lokalisierung;
    @JsonProperty(UserInfoAttributeNames.VERTRAUENSSTUFE)
    private Vertrauensstufe vertrauensstufe;

    @JsonProperty(UserInfoAttributeNames.GRUPPE_REFERRER)
    private GruppenId referrer;

    @JsonIgnore
    public boolean isEmpty()
    {
        return (personName == null || personName.isEmpty()) && (geburt == null || geburt.isEmpty()) && geschlecht == null && lokalisierung == null
                && vertrauensstufe == null && (referrer == null || referrer.isEmpty());
    }
}
