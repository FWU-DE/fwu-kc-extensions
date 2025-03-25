package de.intension.events;

import de.intension.events.publishers.EventPublisher;
import de.intension.events.publishers.dto.BMILoginEvent;
import org.keycloak.events.Event;
import org.keycloak.models.KeycloakSession;

import java.util.Date;

/**
 * Provider for the user login event in Keycloak to be sent to BMI message queue.
 */
public class BMILoginEventListenerProvider
        extends LoginEventListenerProvider {

    public BMILoginEventListenerProvider(KeycloakSession session, EventPublisher publisher, String schoolIdsAttributeName) {
        super(session, publisher, schoolIdsAttributeName);
    }

    @Override
    public void publishEvent(Event event) {
        BMILoginEvent loginEvent = BMILoginEvent.builder()
                .federalState(event.getDetails().get("identity_provider"))
                .timestamp(new Date(event.getTime()))
                .product(event.getClientId())
                .schoolIds(retrieveSchoolIds())
                .build();
        internalPublishEvent(loginEvent);
    }
}
