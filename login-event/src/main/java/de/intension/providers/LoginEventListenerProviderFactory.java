package de.intension.providers;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import com.rabbitmq.client.ConnectionFactory;

import de.intension.publishers.EventPublisher;
import de.intension.publishers.rabbitmq.RabbitMqConnectionManager;
import de.intension.publishers.rabbitmq.RabbitMqEventPublisher;

/**
 * Factory for the user login event listener
 * 
 * @author kdeshpande
 *
 */
public class LoginEventListenerProviderFactory implements EventListenerProviderFactory {

	private EventPublisher publisher = new RabbitMqEventPublisher();

	@Override
	public EventListenerProvider create(KeycloakSession session) {
		return new LoginEventListenerProvider(session, publisher);
	}

	@Override
	public void init(Scope config) {
		RabbitMqConnectionManager.INSTANCE.init(config, new ConnectionFactory());
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {

	}

	@Override
	public void close() {
		this.publisher.close();
	}

	@Override
	public String getId() {
		return "login-event-publisher";
	}

}
