package de.intension.events.publishers.dto;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetailedLoginEvent
{

    @JsonIgnore
    private String       type;

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
