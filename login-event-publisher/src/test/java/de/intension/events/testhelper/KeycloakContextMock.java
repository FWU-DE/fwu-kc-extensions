package de.intension.events.testhelper;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.mockito.Mockito;

public abstract class KeycloakContextMock implements KeycloakContext {

	private RealmModelMock realmFragment;

	public static KeycloakContextMock create() {
		KeycloakContextMock inst;

		inst = Mockito.mock(KeycloakContextMock.class, Mockito.CALLS_REAL_METHODS);
		return init(inst);
	}

	private static KeycloakContextMock init(KeycloakContextMock inst) {
		return inst;
	}

	protected KeycloakContextMock() {
		super();
		init(this);
	}

	public RealmModel getRealm() {
		return this.RealmModelMock();
	}

	public void setRealm(RealmModel realm) {
		this.realmFragment = (RealmModelMock) realm;
	}

	public RealmModelMock RealmModelMock() {
		return this.realmFragment;
	}

}
