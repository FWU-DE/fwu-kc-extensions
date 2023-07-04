package de.intension.publishers.rabbitmq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.keycloak.events.Event;

import de.intension.publishers.EventPublisher;
import de.intension.publishers.dto.EventDTO;

public class RabbitMqEventPublisher implements EventPublisher {

	@Override
	public void publish(Event event) {
		EventDTO eventMessage = new EventDTO();
		eventMessage.setRealmId(event.getRealmId());
		eventMessage.setClientId(event.getClientId());
		eventMessage.setTimeStamp(event.getTime());
		eventMessage.setIdpName(event.getDetails().get("identity_provider"));
		String routingKey = RabbitMqUtils.calculateRoutingKey(event);
		System.out.println("Routing key is " + routingKey);
		String messageString = RabbitMqUtils.writeAsJson(eventMessage, true);
		try {
			RabbitMqConnectionManager.INSTANCE.basicPublish(routingKey, messageString.getBytes(StandardCharsets.UTF_8));
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void close() {
		RabbitMqConnectionManager.INSTANCE.closeConnection();
	}

}
