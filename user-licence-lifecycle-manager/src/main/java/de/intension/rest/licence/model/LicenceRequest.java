package de.intension.rest.licence.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LicenceRequest {

    private String bundesland;
    private String standortnummer;
    private String schulnummer;
    private String userId;
    private String clientName;
}
