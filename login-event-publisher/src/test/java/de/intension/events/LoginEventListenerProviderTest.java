package de.intension.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

import de.intension.events.publishers.EventPublisher;
import de.intension.events.testhelper.KeycloakSessionMock;
import de.intension.events.testhelper.RealmModelMock;
import de.intension.events.testhelper.TestEventFactory;

class LoginEventListenerProviderTest
{

    private final KeycloakSession kcSession = KeycloakSessionMock.create(RealmModelMock.create("realm-test"));
    private TestPublisher         publisher;

    @BeforeEach
    void initPublisher()
        throws Exception
    {
        publisher = new TestPublisher();
    }

    @Test
    void publish_login_events()
    {
        LoginEventListenerProvider provider = new LoginEventListenerProvider(kcSession, publisher);
        Event event = TestEventFactory.create();
        provider.onEvent(event);
        kcSession.getTransactionManager().commit();
        assertThat(publisher.events).hasSize(1)
            .contains(event);
    }

    @Test
    void check_non_login_events()
    {
        LoginEventListenerProvider provider = new LoginEventListenerProvider(kcSession, publisher);
        Event event = TestEventFactory.create(EventType.LOGOUT);
        provider.onEvent(event);
        kcSession.getTransactionManager().commit();
        assertThat(publisher.events).isEmpty();
    }

    private class TestPublisher
        implements EventPublisher
    {

        private final List<Event> events = new ArrayList<>();

        @Override
        public void close()
        {
            // nothing to close
        }

        @Override
        public void publish(Event event)
        {
            events.add(event);
        }

        @Override
        public void initConnection(Scope config)
        {
            // nothing to init
        }
    }

}
