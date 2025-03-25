package de.intension.events.publishers;

import org.keycloak.Config.Scope;

import de.intension.events.publishers.dto.LoginEvent;

public interface EventPublisher {

	void initConnection(Scope config);

    void publish(LoginEvent event);

	void close();
}
