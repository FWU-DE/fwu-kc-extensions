package de.intension.events.publishers;

import org.keycloak.Config.Scope;
import org.keycloak.events.Event;

public interface EventPublisher {

	public void initConnection(Scope config);

	public void publish(Event event);

	public void close();
}
