package de.intension.events.publishers;

import org.keycloak.Config.Scope;

import de.intension.events.publishers.dto.DetailedLoginEvent;

public interface EventPublisher {

	public void initConnection(Scope config);

    public void publish(DetailedLoginEvent event);

	public void close();
}
