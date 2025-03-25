package de.intension.events.publishers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;
import java.util.List;

@Builder
@Getter
public class BMILoginEvent
        implements LoginEvent {

    @JsonProperty("event_type")
    private final String eventType = "vidis_login";
    @JsonProperty("federal_state")
    private String federalState;
    private Date timestamp;
    private String product;
    @JsonProperty("school_ids")
    private List<String> schoolIds;

}
