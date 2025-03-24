package de.intension.events.testhelper;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;

import java.time.Instant;
import java.util.Map;

public class TestEventHelper {

    public static final String REALM_ID = "test-realm";
    public static final EventType EVENT_TYPE = EventType.LOGIN;
    public static final String CLIENT_ID = "account-console";
    public static final Instant TIMESTAMP = Instant.now();
    public static final String IDP_NAME = "test-idp";
    public static final String USER_ID = "idp-user";

    public static Event create() {
        return create(EVENT_TYPE, REALM_ID, CLIENT_ID, TIMESTAMP, IDP_NAME);
    }

    public static Event create(EventType eventType) {
        return create(eventType, REALM_ID, CLIENT_ID, TIMESTAMP, IDP_NAME);
    }

    public static Event create(EventType type, String realm, String clientId, Instant timestamp, String idpName) {
        Event e = new Event();
        e.setType(type);
        e.setRealmId(realm);
        e.setClientId(clientId);
        e.setTime(timestamp.toEpochMilli());
        e.setDetails(Map.of("identity_provider", idpName));
        e.setUserId(USER_ID);
        return e;
    }
}
