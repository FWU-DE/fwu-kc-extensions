package de.intension.id.oidc;

import de.intension.id.PrefixAttributeService;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.intension.id.PrefixAttributeConstants.LOWER_CASE;
import static de.intension.id.PrefixAttributeConstants.PREFIX;

/**
 * Identity provider mapper to map OIDC token claims to user attributes.
 */
public class PrefixAttributeOidcMapper extends AbstractClaimMapper {

    public static final String PROVIDER_ID = "prefixed-attribute-idp-mapper";
    public static final String[] COMPATIBLE_PROVIDERS = {
            KeycloakOIDCIdentityProviderFactory.PROVIDER_ID,
            OIDCIdentityProviderFactory.PROVIDER_ID
    };
    public static final String ATTRIBUTE = "attribute";
    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(CLAIM);
        property.setLabel("Claim");
        property
                .setHelpText("Name of claim to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(PREFIX);
        property.setLabel("Prefix");
        property.setHelpText("Prefix to add to the claim.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(LOWER_CASE);
        property.setLabel("To lowercase");
        property.setHelpText("Transform the attribute value to lowercase.");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue(false);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(ATTRIBUTE);
        property.setLabel("User Attribute Name");
        property.setHelpText("User attribute name to store claim.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        // supports any sync mode
        return true;
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
        return "Prefixed Attribute Mapper";
    }

    @Override
    public String getHelpText() {
        return "This mapper will prefix a claim value and store it in a user attribute.";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                              BrokeredIdentityContext context) {
        prefix(user, mapperModel.getConfig(), context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                                   BrokeredIdentityContext context) {
        prefix(user, mapperModel.getConfig(), context);
    }

    /**
     * Prefixes each attribute in a list with a given prefix.
     *
     * @param user    User to set modified attribute value
     * @param context IdP context to get claim value
     */
    @SuppressWarnings("unchecked")
    private void prefix(UserModel user, Map<String, String> config, BrokeredIdentityContext context) {
        String claim = config.get(CLAIM);
        String userAttribute = config.get(ATTRIBUTE);
        String prefix = config.get(PREFIX);
        boolean toLowerCase = Boolean.parseBoolean(config.getOrDefault(LOWER_CASE, Boolean.FALSE.toString()));
        Object claimValue = getClaimValue(context, claim);
        if (claimValue == null) {
            return;
        }
        PrefixAttributeService prefixer = new PrefixAttributeService(prefix, toLowerCase);
        if (claimValue instanceof List) {
            var prefixedValues = prefixer.prefix((List<String>) claimValue);
            if (!prefixedValues.isEmpty()) {
                user.setAttribute(userAttribute, prefixedValues);
            }
        } else {
            String stringValue = (String) claimValue;
            if (stringValue.isBlank()) {
                return;
            }
            user.setSingleAttribute(userAttribute, prefixer.prefix(stringValue));
        }
    }
}
