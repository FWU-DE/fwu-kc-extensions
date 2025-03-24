package de.intension.events.v2.testhelper;

import org.keycloak.models.RealmModel;
import org.mockito.Mockito;

public abstract class RealmModelMock implements RealmModel {

	private String name;

	public static RealmModelMock create() {
		RealmModelMock inst;

		inst = Mockito.mock(RealmModelMock.class, Mockito.CALLS_REAL_METHODS);
		return init(inst);
	}

	public static RealmModelMock create(String name) {
		RealmModelMock inst;

		inst = create();
		inst.setName(name);
		return inst;
	}

	private static RealmModelMock init(RealmModelMock inst) {
		return inst;
	}

	protected RealmModelMock() {
		super();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

}
