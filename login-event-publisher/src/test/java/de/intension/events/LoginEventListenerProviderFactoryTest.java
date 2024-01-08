package de.intension.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;

import de.intension.events.publishers.rabbitmq.RabbitMqEventPublisher;
import de.intension.events.testhelper.MockScope;

class LoginEventListenerProviderFactoryTest
{

    /**
     * GIVEN: a {@link LoginEventListenerProviderFactory}
     * WHEN: its method create is called to get a provider
     * THEN: the provider is an instance of {@link LoginEventListenerProvider}
     */
    @Test
    void check_Instance_Of_Login_Event()
    {
        LoginEventListenerProviderFactory factory = new LoginEventListenerProviderFactory();

        EventListenerProvider provider = factory
            .create(new DefaultKeycloakSession(new DefaultKeycloakSessionFactory()));

        assertThat(provider).isInstanceOf(LoginEventListenerProvider.class);
    }

    @Test
    void shouldInitConnectionAndEventFactory_whenInit()
        throws IllegalAccessException, NoSuchFieldException
    {
        MockScope config = MockScope.create();
        config.put("schoolid-attribute-name", "schoolid");

        LoginEventListenerProviderFactory factory = new LoginEventListenerProviderFactory();

        factory.init(config);
        Field eventFactory = factory.getClass().getDeclaredField("eventFactory");
        eventFactory.setAccessible(true);
        assertThat(eventFactory.get(factory)).isNotNull().isInstanceOf(DetailedLoginEventFactory.class);
        Field publisher = factory.getClass().getDeclaredField("publisher");
        publisher.setAccessible(true);
        assertThat(publisher.get(factory)).isNotNull().isInstanceOf(RabbitMqEventPublisher.class);
    }

}
