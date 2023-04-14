package de.intension.acronym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Identity provider mapper that will combine the first two letters of the first and last name to a
 * lowercase acronym.
 */
public class AcronymMapper extends AbstractIdentityProviderMapper
{

    public static final String                          PROVIDER_ID             = "acronym-idp-mapper";
    public static final String[]                        COMPATIBLE_PROVIDERS    = {ANY_PROVIDER};

    public static final String                          ATTRIBUTE               = "attribute";
    private static final String                         ATTRIBUTE_DEFAULT_VALUE = "acronym";

    public static final String                          MODIFIER_LOWER_CASE     = "lower case";
    public static final String                          MODIFIER_CAMEL_CASE     = "camel case";

    public static final String                          MODIFIER                = "modifier";

    protected static final List<ProviderConfigProperty> configProperties        = new ArrayList<>();

    private static final Logger                         LOG                     = Logger.getLogger(AcronymMapper.class);

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE);
        property.setLabel("User Attribute");
        property.setHelpText("Name of the user attribute, which will contain the acronym. Defaults to 'acronym' when left empty.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue(ATTRIBUTE_DEFAULT_VALUE);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(MODIFIER);
        property.setLabel("Modifier");
        List<String> options = Arrays.asList(MODIFIER_LOWER_CASE, MODIFIER_CAMEL_CASE);
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(options);
        property.setDefaultValue(MODIFIER_LOWER_CASE);
        property.setHelpText("Defines, how the acronym should be converted ('lower case': Max Muster -> mamu) "
                + "'camel case': Max Muster -> MaMu). Default behaviour is 'lower case'");
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
        var attribute = mapperModel.getConfig().getOrDefault(ATTRIBUTE, ATTRIBUTE_DEFAULT_VALUE);
        String modifier = mapperModel.getConfig().getOrDefault(MODIFIER, MODIFIER_LOWER_CASE);

        var firstName = getFirstName(user);
        var lastName = getLastName(user);

        if (firstName != null && lastName != null) {
            user.setSingleAttribute(attribute, AcronymUtil.createAcronym(firstName, lastName, modifier));
        }
    }

    private static String getFirstName(UserModel user) {
        var firstName = user.getFirstName();
        if (checkValue(user.getId(), firstName, "first name")) {
            return null;
        }
        return firstName;
    }

    private static String getLastName(UserModel user) {
        var lastName = user.getLastName();
        if (checkValue(user.getId(), lastName, "last name")) {
            return null;
        }
        return lastName;
    }

    /**
     * Check whether the given value is null, empty or too short for acronym mapping.
     *
     * @param userId      Used for logging
     * @param value       The value to check
     * @param displayName Used for logging the name of the value
     * @return <b>true</b> if value was null, empty or too short
     * <b>false</b> otherwise
     */
    private static boolean checkValue(String userId, String value, String displayName) {
        if (value == null || value.isBlank()) {
            LOG.warnf("User '%s' is missing %s. Skipping acronym mapping.", userId, displayName);
            return true;
        }
        if (value.length() < 2) {
            LOG.warnf("User '%s' %s '%s' is too short. Skipping acronym mapping.", userId, displayName, value);
            return true;
        }
        return false;
    }
}
