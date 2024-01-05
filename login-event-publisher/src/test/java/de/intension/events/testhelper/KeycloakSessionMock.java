package de.intension.events.testhelper;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.*;
import org.keycloak.provider.Provider;
import org.mockito.Mockito;

public abstract class KeycloakSessionMock implements KeycloakSession {

	private KeycloakContextMock keycloakContext;
	private KeycloakTransactionManager transactionManager;

    private Map<Class, Provider>       providersByClass;

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

    public static KeycloakSessionMock create(RealmModel realm, UserModel user)
    {
        KeycloakSessionMock inst;

        inst = create(realm);
        inst.providersByClass = new HashMap<>();
        inst.providersByClass.put(UserProvider.class, UserProviderMock.create(user));
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

    @Override
    public <T extends Provider> T getProvider(Class<T> aClass, String s)
    {
        return (T)providersByClass.get(aClass);
    }
}
