package de.intension.schoolmapper;

import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PrefixAttributeMapper extends AbstractClaimMapper {
    public static final String PROVIDER_ID = "prefixed-attribute-idp-mapper";
    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    public static final String ATTRIBUTE = "attribute";
    public static final String PREFIX = "prefix";
    public static final String LOWER_CASE = "toLowerCase";

    private final Logger logger = Logger.getLogger(PrefixAttributeMapper.class);

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(CLAIM);
        property.setLabel("Claim");
        property.setHelpText("Name of claim to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
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
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        templateClaim(user, mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        templateClaim(user, mapperModel, context);
    }

    @SuppressWarnings("unchecked")
    private void templateClaim(UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        var config = mapperModel.getConfig();
        String claim = config.get(CLAIM);
        String userAttribute = config.get(ATTRIBUTE);
        String prefix = config.get(PREFIX);
        boolean toLowerCase = Boolean.parseBoolean(config.getOrDefault(LOWER_CASE, Boolean.FALSE.toString()));
        logger.info("Claim: " + claim);
        Object claimValue = getClaimValue(context, claim);
        if (claimValue instanceof List) {
            logger.info(claimValue.toString());
            var claimValues = (List<Object>) claimValue;
            claimValues.forEach(v -> logger.info(v + " is of type " + v.getClass()));
            var prefixed = claimValues.stream()
                    .map(c -> prefix + c)
                    .map(toLowerCase ? String::toLowerCase : c -> c)
                    .collect(Collectors.toList());
            user.setAttribute(userAttribute, prefixed);
        } else {
            logger.info(claimValue + " is of type " + claimValue.getClass());
            String prefixed = prefix + claimValue;
            user.setSingleAttribute(userAttribute, toLowerCase ? prefixed.toLowerCase() : prefixed);
        }
    }
}
