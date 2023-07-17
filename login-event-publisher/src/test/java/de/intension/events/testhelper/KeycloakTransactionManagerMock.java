package de.intension.events.testhelper;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.services.DefaultKeycloakTransactionManager;

public class KeycloakTransactionManagerMock extends DefaultKeycloakTransactionManager {

	public KeycloakTransactionManagerMock(KeycloakSession session) {
		super(session);
	}

	@Override
	public void enlistAfterCompletion(KeycloakTransaction transaction) {
		transaction.begin();
		super.enlistAfterCompletion(transaction);
	}
}