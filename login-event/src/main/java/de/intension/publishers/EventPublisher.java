package de.intension.publishers;

import org.keycloak.events.Event;

public interface EventPublisher {

	public void publish(Event event);

	public void close();

}
