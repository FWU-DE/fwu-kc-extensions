package de.intension.events.testhelper;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmModel;
import org.mockito.Mockito;

public abstract class KeycloakSessionMock implements KeycloakSession {

	private KeycloakContextMock keycloakContext;
	private KeycloakTransactionManager transactionManager;

	public static KeycloakSessionMock create() {
		KeycloakSessionMock inst;

		inst = Mockito.mock(KeycloakSessionMock.class, Mockito.CALLS_REAL_METHODS);
		return init(inst);
	}

	public static KeycloakSessionMock create(RealmModel realm) {
		KeycloakSessionMock inst;

		inst = create();
		inst.getContext().setRealm(realm);
		return inst;
	}

	private static KeycloakSessionMock init(KeycloakSessionMock inst) {
		inst.keycloakContext = KeycloakContextMock.create();
		return inst;
	}

	protected KeycloakSessionMock() {
		super();
		init(this);
	}

	@Override
	public KeycloakContext getContext() {
		return this.getKeycloakContextMock();
	}

	public KeycloakContextMock getKeycloakContextMock() {
		return this.keycloakContext;
	}

	@Override
	public KeycloakTransactionManager getTransactionManager() {
		if (transactionManager == null) {
			transactionManager = new KeycloakTransactionManagerMock(this);
		}
		return transactionManager;
	}

}
