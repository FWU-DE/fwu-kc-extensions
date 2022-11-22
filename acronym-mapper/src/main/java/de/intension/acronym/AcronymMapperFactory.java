package de.intension.acronym;

import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ConfigurationValidationHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapperFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for {@link AcronymMapper}.
 */
public class AcronymMapperFactory extends AbstractLDAPStorageMapperFactory {

    public static final String PROVIDER_ID = "acronym-mapper";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty attrName = createConfigProperty(AcronymMapper.USER_MODEL_ATTRIBUTE, "User Model Attribute Name",
                "Name of the user attribute, which will contain the acronym",
                ProviderConfigProperty.STRING_TYPE, null);
        attrName.setDefaultValue("acronym");
        configProperties.add(attrName);
    }

    @Override
    public String getHelpText() {
        return "This mapper will combine the first two letters of the first and last name to a lowercase acronym.";
    }

    @Override
    public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
            throws ComponentValidationException {
        ConfigurationValidationHelper.check(config)
                .checkRequired(AcronymMapper.USER_MODEL_ATTRIBUTE, "Attribute Name");
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        return new AcronymMapper(mapperModel, federationProvider);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
