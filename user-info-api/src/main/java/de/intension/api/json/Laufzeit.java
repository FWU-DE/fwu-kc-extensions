package de.intension.api.json;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import de.intension.api.UserInfoAttributeNames;
import de.intension.api.enumerations.GermanBoolean;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Laufzeit
{

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(UserInfoAttributeNames.LAUFZEIT_VON)
    LocalDate             von;
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty(UserInfoAttributeNames.LAUFZEIT_BIS)
    LocalDate             bis;
    @JsonProperty(UserInfoAttributeNames.LAUFZEIT_VON_LERN_PERIODE)
    private Lernperiode   vonLernPeriode;
    @JsonProperty(UserInfoAttributeNames.LAUFZEIT_BIS_LERN_PERIODE)
    private Lernperiode   bisLernPeriode;
    @JsonProperty(UserInfoAttributeNames.GRUPPE_SICHT_FREIGABE)
    private GermanBoolean sichtfreigabe;
}
