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
public class Anschrift
{

    @JsonProperty(UserInfoAttributeNames.ORG_ANSCHRIFT_POSTLEITZAHL)
    private String                         postleitzahl;
    @JsonProperty(UserInfoAttributeNames.ORG_ANSCHRIFT_ORT)
    private String                         ort;
    @JsonProperty(UserInfoAttributeNames.ORG_ANSCHRIFT_ORTSTEIL)
    private String                         ortsteil;
    @JsonProperty(UserInfoAttributeNames.ORG_ANSCHRIFT_VERWALTUNGSPOLITISCHE_KODIERUNG)
    private VerwaltungspolitischeKodierung verwaltungspolitischeKodierung;

    @JsonIgnore
    public boolean isEmpty()
    {
        return StringUtil.isBlank(postleitzahl) && StringUtil.isBlank(ort) && StringUtil.isBlank(ortsteil)
                && (verwaltungspolitischeKodierung == null || verwaltungspolitischeKodierung.isEmpty());
    }
}
