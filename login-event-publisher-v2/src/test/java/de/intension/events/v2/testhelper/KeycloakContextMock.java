package de.intension.events.v2.testhelper;

import java.util.Collections;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mockito;

public abstract class KeycloakContextMock implements KeycloakContext {

	private RealmModelMock realmFragment;
    private AuthenticationSessionMock authenticationSessionMock;

	public static KeycloakContextMock create() {
		KeycloakContextMock inst;

		inst = Mockito.mock(KeycloakContextMock.class, Mockito.CALLS_REAL_METHODS);
        inst.authenticationSessionMock = AuthenticationSessionMock.create(UserModelMock.create("test-user", Collections.EMPTY_LIST));
		return init(inst);
    }

    public static KeycloakContextMock create(UserModel user)
    {
        KeycloakContextMock inst;

        inst = Mockito.mock(KeycloakContextMock.class, Mockito.CALLS_REAL_METHODS);
        inst.authenticationSessionMock = AuthenticationSessionMock.create(user);
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

    @Override
    public AuthenticationSessionModel getAuthenticationSession()
    {
        return authenticationSessionMock;
    }
}
