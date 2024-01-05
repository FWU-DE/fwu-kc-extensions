package de.intension.events.publishers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.common.util.Time.currentTimeMillis;
import static org.keycloak.events.EventType.LOGIN;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import de.intension.events.publishers.dto.DetailedLoginEvent;
import de.intension.events.publishers.rabbitmq.RabbitMqConnectionManager;
import de.intension.events.publishers.rabbitmq.RabbitMqEventPublisher;
import de.intension.events.testhelper.MockScope;

class RabbitMqEventPublisherTest
{

	private static ConnectionFactory factory;
	private static Channel channel;
	private static ArgumentCaptor<String> exchangeArg;
	private static ArgumentCaptor<String> routingKeyArg;
	private static ArgumentCaptor<BasicProperties> propsArg;
	private static ArgumentCaptor<byte[]> bodyArg;
	private final long now = currentTimeMillis();

	@BeforeAll
	static void initConnectionManager() throws Exception {
		factory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		when(factory.newConnection()).thenReturn(connection);
		channel = mock(Channel.class);
		when(connection.createChannel()).thenReturn(channel);
		when(channel.exchangeDeclare(Mockito.anyString(), Mockito.any(BuiltinExchangeType.class)))
				.thenReturn(mock(DeclareOk.class));
		when(channel.isOpen()).thenReturn(true);
		exchangeArg = ArgumentCaptor.forClass(String.class);
		routingKeyArg = ArgumentCaptor.forClass(String.class);
		propsArg = ArgumentCaptor.forClass(BasicProperties.class);
		bodyArg = ArgumentCaptor.forClass(byte[].class);
		RabbitMqConnectionManager.INSTANCE.init(MockScope.create().put("rmq-exchange", "login-details"), factory);
	}

	@BeforeEach
    void mockAMQPClient()
    {
		Mockito.clearInvocations(factory, channel);
	}

	@Test
	void publish_login_event() throws Exception {
		RabbitMqEventPublisher publisher = new RabbitMqEventPublisher();

        DetailedLoginEvent event = new DetailedLoginEvent();
        event.setType(LOGIN.toString());
        event.setTimeStamp(new Date(now));
		event.setRealmId("test");
		publisher.publish(event);

		verify(channel).basicPublish(exchangeArg.capture(), routingKeyArg.capture(), propsArg.capture(),
				bodyArg.capture());
		assertThat(exchangeArg.getValue()).isEqualTo("login-details");
		assertThat(routingKeyArg.getValue()).isEqualTo("KC.EVENT.LOGIN");
		assertThat(propsArg.getValue()).isEqualTo(RabbitMqConnectionManager.PERSISTENT_JSON);
	}
}
