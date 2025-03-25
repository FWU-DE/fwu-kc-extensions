package de.intension.events;

import de.intension.events.publishers.EventPublisher;
import de.intension.events.publishers.dto.BMILoginEvent;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import java.time.Instant;
import java.util.List;

/**
 * Provider for the user login event in Keycloak to be sent to BMI message queue.
 */
public class BMILoginEventListenerProvider
        implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(BMILoginEventListenerProvider.class);

    private final KeycloakSession keycloakSession;
    private final EventPublisher publisher;
    private final String schoolIdsAttributeName;

    // Will eventually call the methods publishEvent and publishAdminEvent
    private final EventListenerTransaction tx = new EventListenerTransaction(this::publishAdminEvent,
            this::publishEvent);

    public BMILoginEventListenerProvider(KeycloakSession session, EventPublisher publisher, String schoolIdsAttributeName) {
        this.keycloakSession = session;
        this.publisher = publisher;
        this.schoolIdsAttributeName = schoolIdsAttributeName;
        session.getTransactionManager().enlistAfterCompletion(tx);
        logger.tracef("[%s] instantiated.", this.getClass());
    }

    @Override
    public void close() {
        // Nothing to do here
    }

    // On the LOGIN event add event in the pool which will eventually be published
    // by the method publishEvent
    @Override
    public void onEvent(Event event) {
        EventType eventType = event.getType();
        if (EventType.LOGIN.equals(eventType)) {
            tx.addEvent(event);
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Nothing to do in the admin events
    }

    private void publishEvent(Event event) {
        BMILoginEvent loginEvent = BMILoginEvent.builder()
                .federalState(event.getDetails().get("identity_provider"))
                .timestamp(Instant.ofEpochMilli(event.getTime()))
                .product(event.getClientId())
                .schoolIds(retrieveSchoolIds())
                .build();
        this.publisher.publish(loginEvent);
    }

    private List<String> retrieveSchoolIds() {
        return keycloakSession.getContext().getAuthenticationSession().getAuthenticatedUser()
                .getAttributeStream(schoolIdsAttributeName).toList();
    }

    private void publishAdminEvent(AdminEvent event, boolean includeRepresentation) {
        // Nothing to do in the admin events
    }
}
