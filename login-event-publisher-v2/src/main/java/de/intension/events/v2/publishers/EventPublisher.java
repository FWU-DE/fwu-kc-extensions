package de.intension.events.v2.publishers;

import org.keycloak.Config.Scope;

import de.intension.events.v2.publishers.dto.DetailedLoginEvent;

public interface EventPublisher {

	public void initConnection(Scope config);

    public void publish(DetailedLoginEvent event);

	public void close();
}
