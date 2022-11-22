package de.intension.schoolmapper;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.broker.oidc.mappers.AbstractClaimMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class TemplatedAttributeMapper extends AbstractClaimMapper {
    public static final String PROVIDER_ID = "templated-attribute-idp-mapper";
    public static final String[] COMPATIBLE_PROVIDERS = {ANY_PROVIDER};

    protected static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    private String claim;
    public static final String USER_ATTRIBUTE = "user.attribute";
    private String userAttribute;
    public static final String TEMPLATE = "template";
    private String template;
    public static final String LOWER_CASE = "toLowerCase";
    private boolean toLowerCase = false;

    private final Logger logger = Logger.getLogger(TemplatedAttributeMapper.class);

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(CLAIM);
        property.setLabel("Claim");
        property.setHelpText("Name of claim to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(TEMPLATE);
        property.setLabel("Template");
        property.setHelpText("Template to parse claim. Values like 'hardcoded.${attribute}' will be parsed using the user's attributes.");
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
        property.setName(USER_ATTRIBUTE);
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
        return "Templated Attribute Mapper";
    }

    @Override
    public String getHelpText() {
        return "This mapper will interpret a template string to an attribute value.";
    }

    @Override
    public void init(Config.Scope config) {
        this.claim = config.get(CLAIM);
        this.userAttribute = config.get(USER_ATTRIBUTE);
        this.template = config.get(TEMPLATE);
        this.toLowerCase = config.getBoolean(LOWER_CASE, false);
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        templateClaim(session, user, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {
        templateClaim(session, user, context);
    }

    private void templateClaim(KeycloakSession session, UserModel user, BrokeredIdentityContext context) {
        try {
            Object claimValue = getClaimValue(context, claim);
            Template freemarker = new Template("tmp", template, null);
            Writer out = new StringWriter();
            freemarker.process(user.getAttributes(), out);
            logger.info(out.toString());
        } catch (IOException | TemplateException e) {
            throw new RuntimeException(e);
        }
    }
}
