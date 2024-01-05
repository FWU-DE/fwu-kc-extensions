package de.intension.events;

import java.util.Date;
import java.util.List;

import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import de.intension.events.publishers.dto.DetailedLoginEvent;

public class DetailedLoginEventFactory
{

    public static final String SCHOOLIDS_ATTRIBUTE_CONFIG_KEY = "schoolids-attribute-key";

    private String             schoolIdsAttributeKey;

    public DetailedLoginEventFactory(String schoolIdsAttributeKey)
    {
        this.schoolIdsAttributeKey = schoolIdsAttributeKey;
    }

    public DetailedLoginEvent create(Event event, KeycloakSession session)
    {
        DetailedLoginEvent detailedLoginEvent = new DetailedLoginEvent();
        detailedLoginEvent.setType(EventType.LOGIN.toString());
        detailedLoginEvent.setRealmId(event.getRealmId());
        detailedLoginEvent.setClientId(event.getClientId());
        detailedLoginEvent.setTimeStamp(new Date(event.getTime()));
        detailedLoginEvent.setIdpName(event.getDetails().get("identity_provider"));
        List<String> schoolIDs = retrieveSchoolIds(session, event.getUserId());
        detailedLoginEvent.setSchoolIds(schoolIDs);
        return detailedLoginEvent;
    }

    private List<String> retrieveSchoolIds(KeycloakSession session, String userId)
    {
        RealmModel realm = session.getContext().getRealm();
        UserProvider userProvider = session.getProvider(UserProvider.class, "jpa");
        UserModel user = userProvider.getUserById(realm, userId);
        return user.getAttributeStream(schoolIdsAttributeKey).toList();
    }
}
