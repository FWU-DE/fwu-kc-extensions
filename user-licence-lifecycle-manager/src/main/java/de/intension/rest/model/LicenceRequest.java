package de.intension.rest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LicenceRequest {

    private String userId;
    private String clientId;
    private String schulkennung;
    private String bundesland;

    public LicenceRequest(String userId, String clientId, String schulkennung, String bundesland) {
        this.userId = userId;
        this.clientId = clientId;
        this.schulkennung = schulkennung;
        this.bundesland = bundesland;
    }
}
