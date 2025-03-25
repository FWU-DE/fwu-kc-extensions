package de.intension.events.publishers.rabbitmq;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public enum RabbitMqConnectionManager {

    INSTANCE;

    public static final BasicProperties PERSISTENT_JSON = new BasicProperties("application/json", null, null, 2, 0,
            null, null, null, null, null, null, null, null, null);
    private static final Logger logger = Logger.getLogger(RabbitMqConnectionManager.class);
    private String exchangeName;
    private String routingKey;
    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;

    // Initiates and configures the connection factory using the environment
    // variables
    public void init(Scope config, ConnectionFactory connectionFactory) {
        this.exchangeName = config.get("rmq-exchange", "login-details");
        this.routingKey = config.get("rmq-routing-key", "KC.EVENT.LOGIN");
        this.connectionFactory = connectionFactory;
        this.connectionFactory.setUsername(config.get("rmq-username", "guest"));
        this.connectionFactory.setPassword(config.get("rmq-password", "guest"));
        this.connectionFactory.setVirtualHost(config.get("rmq-vhost", "/"));
        this.connectionFactory.setHost(config.get("rmq-host", "localhost"));
        this.connectionFactory.setPort(Integer.parseInt(config.get("rmq-port", "5672")));
        this.connectionFactory.setAutomaticRecoveryEnabled(true);
        logger.info("init of Rabbitmq successful");
    }

    /**
     * Publish to exchange with routing key override.
     */
    public void basicPublish(String routingKeyOverride, byte[] bytes) throws IOException {
        String routingKey = routingKeyOverride != null ? routingKeyOverride : this.routingKey;
        Channel ch = getChannel();
        if (ch != null && ch.isOpen()) {
            ch.basicPublish(exchangeName, routingKey, PERSISTENT_JSON, bytes);
        } else {
            logger.warnf("Channel %d is closed! Reason: %s", channel == null ? -1 : channel.getChannelNumber(),
                    channel == null ? "" : channel.getCloseReason().getMessage());
        }
    }

    public void closeConnection() {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            logger.error("keycloak-to-rabbitmq ERROR on close", e);
        }
    }

    public Channel getChannel() {
        if (channel == null) {
            channel = createChannel();
        } else if (!channel.isOpen()) {
            logger.warnf("Channel %s is closed! Reason: %s", channel.getChannelNumber(),
                    channel.getCloseReason().getReason());
            channel = createChannel();
        }
        return channel;
    }

    private Channel createChannel() {
        Channel ch = null;
        try {
            Connection c = getConnection();
            if (c != null) {
                ch = c.createChannel();
                ch.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC, true);
            }
        } catch (IOException e) {
            logger.errorf("Unable to create channel: %s", e.getMessage(), e);
        }
        return ch;
    }

    private Connection getConnection() {
        if (connection == null) {
            connection = createConnection();
        } else if (!connection.isOpen()) {
            logger.warnf("Connection is closed! Reason: %s", connection.getCloseReason().getMessage());
            connection = createConnection();
        }
        return connection;
    }

    private Connection createConnection() {
        try {
            return connectionFactory.newConnection();
        } catch (IOException | TimeoutException exception) {
            logger.errorf(exception, "Unable to open new connection: hostname: %s, username: %s and portnumber: %d",
                    this.connectionFactory.getHost(), this.connectionFactory.getUsername(),
                    this.connectionFactory.getPort(), exception);
        }
        return null;
    }
}
