package de.intension.events.publishers.rabbitmq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.util.JsonSerialization;

import com.rabbitmq.client.ConnectionFactory;

import de.intension.events.exception.LoginEventException;
import de.intension.events.publishers.EventPublisher;
import de.intension.events.publishers.dto.EventDTO;

public class RabbitMqEventPublisher implements EventPublisher {

	private static final Logger logger = Logger.getLogger(RabbitMqEventPublisher.class);
	public static final String ROUTING_KEY_PREFIX = "KC.EVENT";
	public static final String PERIOD_SEPARATOR = ".";

	@Override
	public void publish(Event event) {
		EventDTO eventDetails = populateEventDetails(event);
		String routingKey = calculateRoutingKey(event);

		try {
			String messageString = writeAsJson(eventDetails);
			RabbitMqConnectionManager.INSTANCE.basicPublish(routingKey, messageString.getBytes(StandardCharsets.UTF_8));
		} catch (IOException ex) {
			throw new LoginEventException("Error while publishing the message to the queue", ex);
		}

	}

	@Override
	public void close() {
		RabbitMqConnectionManager.INSTANCE.closeConnection();
	}

	@Override
	public void initConnection(Scope config) {
		RabbitMqConnectionManager.INSTANCE.init(config, new ConnectionFactory());
	}

	private EventDTO populateEventDetails(Event event) {
		EventDTO eventMessage = new EventDTO();
		eventMessage.setRealmId(event.getRealmId());
		eventMessage.setClientId(event.getClientId());
		eventMessage.setTimeStamp(new Date(event.getTime()));
		eventMessage.setIdpName(event.getDetails().get("identity_provider"));
		return eventMessage;
	}

	// Returns routing key in the format KC.EVENT.EVENT_TYPE
	public static String calculateRoutingKey(Event event) {
		String routingKey = ROUTING_KEY_PREFIX + PERIOD_SEPARATOR + event.getType();
		return routingKey;
	}

	public static String writeAsJson(Object object) throws IOException {
		try {
			return JsonSerialization.writeValueAsString(object);
		} catch (IOException e) {
			logger.error("Could not serialize to JSON", e);
			throw new IOException(e);
		}
	}
}
