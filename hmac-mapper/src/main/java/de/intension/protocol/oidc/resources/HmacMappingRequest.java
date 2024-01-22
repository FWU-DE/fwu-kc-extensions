package de.intension.protocol.oidc.resources;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HmacMappingRequest {
    private String clientId;
    private List<String> originalIds;
    private String testId;
}
