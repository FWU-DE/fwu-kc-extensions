package de.intension.events.publishers.rabbitmq;

import com.rabbitmq.client.ConnectionFactory;
import de.intension.events.exception.LoginEventException;
import de.intension.events.publishers.EventPublisher;
import de.intension.events.publishers.dto.DetailedLoginEvent;
import de.intension.events.publishers.dto.LoginEvent;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RabbitMqEventPublisher implements EventPublisher {

    private RabbitMqConnectionManager connectionManager;
    
    private static final Logger logger = Logger.getLogger(RabbitMqEventPublisher.class);
    public static final String ROUTING_KEY_PREFIX = "KC.EVENT.";

    // Returns routing key in the format KC.EVENT.EVENT_TYPE
    public static String calculateRoutingKey(LoginEvent event) {
        return event instanceof DetailedLoginEvent ? ROUTING_KEY_PREFIX + ((DetailedLoginEvent) event).getType() : null;
    }

    @Override
    public void close() {
        connectionManager.closeConnection();
    }

    @Override
    public void initConnection(Scope config) {
        initConnection(config, new ConnectionFactory());
    }

    public void initConnection(Scope config, ConnectionFactory factory) {
        connectionManager = new RabbitMqConnectionManager();
        connectionManager.init(config, factory);
    }

    @Override
    public void publish(LoginEvent event) {
        String routingKey = calculateRoutingKey(event);

        try {
            String messageString = writeAsJson(event);
            connectionManager.basicPublish(routingKey, messageString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new LoginEventException("Error while publishing the message to the queue", ex);
        }
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
