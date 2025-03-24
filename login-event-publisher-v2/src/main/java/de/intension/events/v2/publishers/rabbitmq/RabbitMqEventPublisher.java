package de.intension.events.v2.publishers.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.ConnectionFactory;
import de.intension.events.v2.exception.LoginEventException;
import de.intension.events.v2.publishers.EventPublisher;
import de.intension.events.v2.publishers.dto.DetailedLoginEvent;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RabbitMqEventPublisher implements EventPublisher {

	private static final Logger logger = Logger.getLogger(RabbitMqEventPublisher.class);

	private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

	@Override
	public void close() {
		RabbitMqConnectionManager.INSTANCE.closeConnection();
	}

	@Override
	public void initConnection(Scope config) {
		RabbitMqConnectionManager.INSTANCE.init(config, new ConnectionFactory());
	}

    @Override
    public void publish(DetailedLoginEvent event)
    {
        try {
            String messageString = writeAsJson(event);
            RabbitMqConnectionManager.INSTANCE.basicPublish(messageString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new LoginEventException("Error while publishing the message to the queue", ex);
        }

	}

	public static String writeAsJson(Object object) throws IOException {
		try {
			return mapper.writeValueAsString(object);
		} catch (IOException e) {
			logger.error("Could not serialize to JSON", e);
			throw new IOException(e);
		}
	}
}
