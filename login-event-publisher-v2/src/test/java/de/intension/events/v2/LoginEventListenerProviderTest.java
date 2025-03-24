package de.intension.events.v2;

import de.intension.events.v2.publishers.EventPublisher;
import de.intension.events.v2.publishers.dto.DetailedLoginEvent;
import de.intension.events.v2.testhelper.KeycloakSessionMock;
import de.intension.events.v2.testhelper.RealmModelMock;
import de.intension.events.v2.testhelper.TestEventHelper;
import de.intension.events.v2.testhelper.UserModelMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoginEventListenerProviderTest
{

    public static final String SCHOOL_IDS_ATTRIBUTE_NAME = "schulkennung";
    private KeycloakSession    kcSession;
    private TestPublisher      publisher;

    @BeforeEach
    void init()
    {
        publisher = new TestPublisher();
        kcSession = KeycloakSessionMock
            .create(RealmModelMock.create("realm-test"), UserModelMock.create("idp-user", Arrays.asList("DE_BY-1234", "DE_BY-4321")));
    }

    @Test
    void publish_login_events()
    {
        LoginEventListenerProvider provider = new LoginEventListenerProvider(kcSession, publisher, SCHOOL_IDS_ATTRIBUTE_NAME);
        Event event = TestEventHelper.create();
        provider.onEvent(event);
        kcSession.getTransactionManager().commit();
        assertThat(publisher.events).hasSize(1);
        DetailedLoginEvent actual = publisher.events.get(0);
        assertThat(actual.getEventType()).isEqualTo("vidis_login");
        assertThat(actual.getTimestamp()).isEqualTo(Instant.ofEpochMilli(TestEventHelper.TIMESTAMP.toEpochMilli()));
        assertThat(actual.getFederalState()).isEqualTo(TestEventHelper.IDP_NAME);
        assertThat(actual.getSchoolIds()).hasSize(2).containsExactlyInAnyOrder("DE_BY-1234", "DE_BY-4321");
        assertThat(actual.getProduct()).isEqualTo(TestEventHelper.CLIENT_ID);
    }

    @Test
    void check_non_login_events()
    {
        LoginEventListenerProvider provider = new LoginEventListenerProvider(kcSession, publisher, SCHOOL_IDS_ATTRIBUTE_NAME);
        Event event = TestEventHelper.create(EventType.LOGOUT);
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
