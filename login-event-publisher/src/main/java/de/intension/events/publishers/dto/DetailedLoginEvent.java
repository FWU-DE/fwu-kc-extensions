package de.intension.events.publishers.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * @deprecated since 5.0.2, use {@link BMILoginEvent} instead
 */
@Getter
@Setter
@Deprecated(since = "5.0.2", forRemoval = true)
public class DetailedLoginEvent
        implements LoginEvent {

    @JsonIgnore
    private String type;

    @JsonProperty("realmId")
    private String realmId;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("timeStamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date timeStamp;

    @JsonProperty("idpName")
    private String idpName;

    @JsonProperty
    private List<String> schoolIds;

}
