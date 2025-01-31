package de.intension.rest.licence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LicenceRequest {

    private String userId;
    private String clientId;
    private String schulkennung;
    private String bundesland;
}
