package de.intension.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.keycloak.events.EventType.LOGIN;
import static org.keycloak.events.EventType.LOGOUT;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.Config.Scope;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;

import de.intension.events.LoginEventListenerProvider;
import de.intension.events.publishers.EventPublisher;
import de.intension.events.testhelper.KeycloakSessionMock;
import de.intension.events.testhelper.RealmModelMock;

public class LoginEventListenerProviderTest {

	private final KeycloakSession kcSession = KeycloakSessionMock.create(RealmModelMock.create("realm-test"));
	private TestPublisher publisher;

	private static Event event(EventType type) {
		Event e = new Event();
		e.setType(type);
		return e;
	}

	@BeforeEach
	void initPublisher() throws Exception {
		publisher = new TestPublisher();
	}

	@Test
	void publish_login_events() {
		LoginEventListenerProvider provider = new LoginEventListenerProvider(kcSession, publisher);
		Event event = event(LOGIN);
		provider.onEvent(event);
		kcSession.getTransactionManager().commit();
		assertThat(publisher.events.size()).isEqualTo(1);
		assertThat(publisher.events).contains(event);
	}

	@Test
	void check_non_login_events() {
		LoginEventListenerProvider provider = new LoginEventListenerProvider(kcSession, publisher);
		Event event = event(LOGOUT);
		provider.onEvent(event);
		kcSession.getTransactionManager().commit();
		assertThat(publisher.events).isEmpty();
	}

	private class TestPublisher implements EventPublisher {

		private final List<Event> events = new ArrayList<>();

		@Override
		public void close() {
			// nothing to close
		}

		@Override
		public void publish(Event event) {
			events.add(event);
		}

		@Override
		public void initConnection(Scope config) {
			// nothing to init
		}
	}

}
