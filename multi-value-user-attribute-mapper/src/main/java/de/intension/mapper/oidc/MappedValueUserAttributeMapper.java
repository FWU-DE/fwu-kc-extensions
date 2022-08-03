package de.intension.mapper.oidc;

import static de.intension.mapper.saml.MappedValueUserAttributeMapper.*;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Identity provider mapper which imports incoming IdP attributes/claims to user attributes. In
 * addition, incoming values
 * can be translated to other values.
 */
public class MappedValueUserAttributeMapper
        extends UserAttributeMapper
{

    private static final List<ProviderConfigProperty> customConfigProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(VALUE_MAPPINGS);
        property.setLabel(FIELD_LABEL);
        property.setHelpText(FIELD_HELP_TEXT);
        property.setType(ProviderConfigProperty.MAP_TYPE);
        customConfigProperties.add(property);
    }

    private final String providerId      = PROVIDER_ID_PREFIX.concat(super.getId());
    private final String typeAndCategory = TYPE_AND_CATEGORY_PREFIX.concat(super.getDisplayCategory());

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        List<ProviderConfigProperty> mergedConfig = new ArrayList<>();
        mergedConfig.addAll(super.getConfigProperties());
        mergedConfig.addAll(customConfigProperties);
        return mergedConfig;
    }

    @Override
    public String getId()
    {
        return providerId;
    }

    @Override
    public String getDisplayCategory()
    {
        return typeAndCategory;
    }

    @Override
    public String getDisplayType()
    {
        return getDisplayCategory();
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel,
                                            BrokeredIdentityContext context)
    {
        super.preprocessFederatedIdentity(session, realm, mapperModel, context);
        de.intension.mapper.saml.MappedValueUserAttributeMapper.setMappedValueInContext(mapperModel, context);
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                                   BrokeredIdentityContext context)
    {
        super.updateBrokeredUser(session, realm, user, mapperModel, context);
        de.intension.mapper.saml.MappedValueUserAttributeMapper.setMappedValueAsUserAttribute(user, mapperModel, context);
    }
}
