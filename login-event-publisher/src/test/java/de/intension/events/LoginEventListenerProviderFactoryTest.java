package de.intension.events;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;

public class LoginEventListenerProviderFactoryTest {

	/**
	 * GIVEN: a {@link LoginEventListenerProviderFactory} 
	 * WHEN: its method create is called to get a provider 
	 * THEN: the provider is an instance of {@link LoginEventListenerProvider}
	 */
	@Test
	void check_Instance_Of_Login_Event() {
		LoginEventListenerProviderFactory factory = new LoginEventListenerProviderFactory();

		EventListenerProvider provider = factory
				.create(new DefaultKeycloakSession(new DefaultKeycloakSessionFactory()));

		assertThat(provider).isInstanceOf(LoginEventListenerProvider.class);

	}

}
