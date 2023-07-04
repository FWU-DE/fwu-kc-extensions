package de.intension.publishers.rabbitmq;

import static org.jboss.logging.Logger.getLogger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public enum RabbitMqConnectionManager {

	INSTANCE;

	private static final Logger logger = getLogger(RabbitMqConnectionManager.class);

	public static final BasicProperties PERSISTENT_JSON = new BasicProperties("application/json", null, null, 2, 0,
			null, null, null, null, null, null, null, null, null);

	private ConnectionFactory connectionFactory;
	private Connection connection;
	private Channel channel;

	private synchronized void checkOrCreateConnectionAndChannel() {
		try {
			if (connection == null || !connection.isOpen()) {
				this.connection = connectionFactory.newConnection();
			}
			if (channel == null || !channel.isOpen()) {
				channel = connection.createChannel();
			}
		} catch (IOException | TimeoutException e) {
			logger.error("keycloak-to-rabbitmq ERROR on connection to rabbitmq", e);
		}
	}

	public void init(Scope config, ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
		this.connectionFactory.setUsername(RabbitMqUtils.resolveConfigVar(config, "username", "admin"));
		this.connectionFactory.setPassword(RabbitMqUtils.resolveConfigVar(config, "password", "admin"));
		this.connectionFactory.setVirtualHost(RabbitMqUtils.resolveConfigVar(config, "vhost", "/"));
		this.connectionFactory.setHost(RabbitMqUtils.resolveConfigVar(config, "url", "localhost"));
		this.connectionFactory.setPort(Integer.valueOf(RabbitMqUtils.resolveConfigVar(config, "port", "5672")));
		this.connectionFactory.setAutomaticRecoveryEnabled(true);

		if (Boolean.valueOf("false")) {
			try {
				this.connectionFactory.useSslProtocol();
			} catch (Exception e) {
				logger.error("Could not use SSL protocol", e);
			}
		}
	}

	public void basicPublish(String routingKey, byte[] bytes) throws IOException {
		checkOrCreateConnectionAndChannel();
		if (channel != null && channel.isOpen()) {
			channel.basicPublish("intension-exchange", routingKey, PERSISTENT_JSON, bytes);
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
}
