package de.intension.events.v2.testhelper;

import java.util.Properties;
import java.util.Set;

import org.keycloak.Config.Scope;

public class MockScope implements Scope {

	private final Properties data;

	public MockScope() {
		this(new Properties());
	}

	public MockScope(Properties data) {
		super();
		this.data = data;
	}

	public static MockScope create() {
		return new MockScope();
	}

	@Override
	public String get(String key) {
		return this.getData().getProperty(key);
	}

	@Override
	public String get(String key, String defaultValue) {
		return this.getData().getProperty(key, defaultValue);
	}

	@Override
	public String[] getArray(String key) {
		String string = get(key);
		if (string == null) {
			return null;
		}
		return string.split(",");
	}

	@Override
	public Integer getInt(String key) {
		return getInt(key, null);
	}

	@Override
	public Integer getInt(String key, Integer defaultValue) {
		String value = this.get(key);
		if (value == null) {
			return defaultValue;
		}
		return Integer.valueOf(value);
	}

	@Override
	public Long getLong(String key) {
		return this.getLong(key, null);
	}

	@Override
	public Long getLong(String key, Long defaultValue) {
		String value = this.get(key);
		if (value == null) {
			return defaultValue;
		}
		return Long.valueOf(value);
	}

	@Override
	public Boolean getBoolean(String key) {
		return this.getBoolean(key, null);
	}

	@Override
	public Boolean getBoolean(String key, Boolean defaultValue) {
		String value = this.get(key);
		if (value == null) {
			return defaultValue;
		}
		return "true".equalsIgnoreCase(value);
	}

	@Override
	public Scope scope(String... scope) {
		// Auto-generated method stub
		return null;
	}

	public MockScope put(String name, String value) {
		getData().setProperty(name, value);
		return this;
	}

	public Properties getData() {
		return this.data;
	}

	@Override
	public Set<String> getPropertyNames() {
		throw new UnsupportedOperationException("Not implemented");
	}
}
