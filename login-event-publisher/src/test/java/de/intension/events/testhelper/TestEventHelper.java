package de.intension.events.testhelper;

import java.util.Date;
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;

public class TestEventHelper
{

    public static final String    REALM_ID   = "test-realm";
    public static final EventType EVENT_TYPE = EventType.LOGIN;
    public static final String    CLIENT_ID  = "account-console";
    public static final Date      TIMESTAMP  = new Date();
    public static final String    IDP_NAME   = "test-idp";
    public static final String    USER_ID    = "idp-user";

    public static Event create()
    {
        return create(EVENT_TYPE, REALM_ID, CLIENT_ID, TIMESTAMP, IDP_NAME);
    }

    public static Event create(EventType eventType)
    {
        return create(eventType, REALM_ID, CLIENT_ID, TIMESTAMP, IDP_NAME);
    }

    public static Event create(EventType type, String realm, String clientId, Date timestamp, String idpName)
    {
        Event e = new Event();
        e.setType(type);
        e.setRealmId(realm);
        e.setClientId(clientId);
        e.setTime(timestamp.getTime());
        e.setDetails(Map.of("identity_provider", idpName));
        e.setUserId(USER_ID);
        return e;
    }
}