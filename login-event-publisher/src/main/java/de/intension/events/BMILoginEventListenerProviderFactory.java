package de.intension.events;

import org.keycloak.events.EventListenerProvider;
import org.keycloak.models.KeycloakSession;

public class BMILoginEventListenerProviderFactory
        extends LoginEventListenerProviderFactory {

    public static final String PROVIDER_ID = "login-event-publisher-bmi";

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new BMILoginEventListenerProvider(session, publisher, schoolIdsAttributeName);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
