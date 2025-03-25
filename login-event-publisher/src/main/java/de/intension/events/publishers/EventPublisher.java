package de.intension.events.publishers;

import org.keycloak.Config.Scope;

import de.intension.events.publishers.dto.LoginEvent;

public interface EventPublisher {

	public void initConnection(Scope config);

    public void publish(LoginEvent event);

	public void close();
}
