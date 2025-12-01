package de.intension.events.publishers.dto;

import org.keycloak.events.Event;

import java.util.Date;
import java.util.List;

@Deprecated(since = "5.0.2", forRemoval = true)
public final class DetailedLoginEventBuilder {

    private String type;
    private String realmId;
    private String clientId;
    private Date timeStamp;
    private String idpName;
    private List<String> schoolIds;

    private DetailedLoginEventBuilder() {
    }

    public static DetailedLoginEventBuilder builder() {
        return new DetailedLoginEventBuilder();
    }

    public static DetailedLoginEventBuilder fromKeycloakEvent(Event event) {
        return builder()
                .withType(event.getType().toString())
                .withRealmId(event.getRealmId())
                .withClientId(event.getClientId())
                .withTimeStamp(new Date(event.getTime()))
                .withIdpName(event.getDetails().get("identity_provider"));
    }

    public DetailedLoginEventBuilder withType(String type) {
        this.type = type;
        return this;
    }

    public DetailedLoginEventBuilder withRealmId(String realmId) {
        this.realmId = realmId;
        return this;
    }

    public DetailedLoginEventBuilder withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public DetailedLoginEventBuilder withTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
        return this;
    }

    public DetailedLoginEventBuilder withIdpName(String idpName) {
        this.idpName = idpName;
        return this;
    }

    public DetailedLoginEventBuilder withSchoolIds(List<String> schoolIds) {
        this.schoolIds = schoolIds;
        return this;
    }

    public DetailedLoginEvent build() {
        DetailedLoginEvent detailedLoginEvent = new DetailedLoginEvent();
        detailedLoginEvent.setType(type);
        detailedLoginEvent.setRealmId(realmId);
        detailedLoginEvent.setClientId(clientId);
        detailedLoginEvent.setTimeStamp(timeStamp);
        detailedLoginEvent.setIdpName(idpName);
        detailedLoginEvent.setSchoolIds(schoolIds);
        return detailedLoginEvent;
    }
}
