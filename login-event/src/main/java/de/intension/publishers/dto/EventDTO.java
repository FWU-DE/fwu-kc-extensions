package de.intension.publishers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventDTO {

	private String realmId;

	private String clientId;

	private long timeStamp;

	private String idpName;

}
