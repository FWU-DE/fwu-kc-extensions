package de.intension.events;

import static de.intension.events.DetailedLoginEventFactory.SCHOOLIDS_ATTRIBUTE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

import de.intension.events.publishers.EventPublisher;
import de.intension.events.publishers.dto.DetailedLoginEvent;
import de.intension.events.testhelper.*;

class LoginEventListenerProviderTest
{

    private final KeycloakSession     kcSession = KeycloakSessionMock
        .create(RealmModelMock.create("realm-test"), UserModelMock.create(TestEventFactory.USER_ID, Arrays.asList("DE_BY-1234", "DE_BY-4321")));
    private TestPublisher             publisher;
    private DetailedLoginEventFactory eventFactory;

    @BeforeEach
    void init()
    {
        publisher = new TestPublisher();
        MockScope config = MockScope.create();
        config.put(SCHOOLIDS_ATTRIBUTE, "schulkennung");
        eventFactory = new DetailedLoginEventFactory(config);
    }

    @Test
    void publish_login_events()
    {
        LoginEventListenerProvider provider = new LoginEventListenerProvider(kcSession, publisher, eventFactory);
        Event event = TestEventFactory.create();
        provider.onEvent(event);
        kcSession.getTransactionManager().commit();
        assertThat(publisher.events).hasSize(1)
            .first().usingRecursiveComparison().ignoringFields("timeStamp", "idpName", "schoolIds", "type").isEqualTo(event);
        DetailedLoginEvent actual = publisher.events.get(0);
        assertThat(actual.getType()).isEqualTo("LOGIN");
        assertThat(actual.getTimeStamp()).isEqualTo(TestEventFactory.TIMESTAMP);
        assertThat(actual.getIdpName()).isEqualTo(TestEventFactory.IDP_NAME);
        assertThat(actual.getSchoolIds()).hasSize(2).containsExactlyInAnyOrder("DE_BY-1234", "DE_BY-4321");
    }

    @Test
    void check_non_login_events()
    {
        LoginEventListenerProvider provider = new LoginEventListenerProvider(kcSession, publisher, eventFactory);
        Event event = TestEventFactory.create(EventType.LOGOUT);
        provider.onEvent(event);
        kcSession.getTransactionManager().commit();
        assertThat(publisher.events).isEmpty();
    }

    private class TestPublisher
        implements EventPublisher
    {

        private final List<DetailedLoginEvent> events = new ArrayList<>();

        @Override
        public void close()
        {
            // nothing to close
        }

        @Override
        public void publish(DetailedLoginEvent event)
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
