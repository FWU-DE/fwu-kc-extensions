package de.intension.acronym;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Identity provider mapper that will combine the first two letters of the first and last name to a lowercase acronym.
 */
public class AcronymMapper extends AbstractIdentityProviderMapper {

    public static final String PROVIDER_ID = "acronym-idp-mapper";
    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};

    public static final String ATTRIBUTE = "attribute";
    private static final String ATTRIBUTE_DEFAULT_VALUE = "acronym";

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private static final Logger LOG = Logger.getLogger(AcronymMapper.class);

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE);
        property.setLabel("User Attribute");
        property.setHelpText("Name of the user attribute, which will contain the acronym. Defaults to 'acronym' when left empty.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(ATTRIBUTE_DEFAULT_VALUE);
        configProperties.add(property);
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        // supports any sync mode
        return true;
    }

    @Override
    public String getHelpText() {
        return "This mapper will combine the first two letters of the first and last name to a lowercase acronym.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Attribute Importer";
    }

    @Override
    public String getDisplayType() {
        return "Acronym";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        updateUser(user, mapperModel);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        updateUser(user, mapperModel);
    }

    private void updateUser(UserModel user, IdentityProviderMapperModel mapperModel) {
        String attribute = mapperModel.getConfig().getOrDefault(ATTRIBUTE, ATTRIBUTE_DEFAULT_VALUE);

        var firstName = getAttribute(user, UserModel.FIRST_NAME);
        var lastName = getAttribute(user, UserModel.LAST_NAME);

        if (firstName != null && lastName != null) {
            user.setSingleAttribute(attribute, AcronymUtil.createAcronym(firstName, lastName));
        }
    }

    /**
     * Get attribute value from a user for acronym creation.
     *
     * @param attribute Name of the attribute to fetch
     * @return Attribute value or null if attribute is missing, empty or shorter than 2 characters
     */
    private String getAttribute(UserModel user, String attribute) {
        var value = user.getAttributeStream(attribute).findFirst().orElse(null);
        if (value == null || value.isBlank()) {
            LOG.warnf("User '%s' is missing attribute value '%s'. Skipping acronym mapping.", user.getId(), attribute);
            return null;
        }
        if (value.length() < 2) {
            LOG.warnf("User '%s' attribute '%s' value '%s' is too short. Skipping acronym mapping.", user.getId(), attribute, value);
            return null;
        }
        return value;
    }
}
