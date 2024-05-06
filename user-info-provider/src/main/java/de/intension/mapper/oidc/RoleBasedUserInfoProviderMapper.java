package de.intension.mapper.oidc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import de.intension.mapper.user.UserInfoHelper;

/**
 * Provides a custom implementation based on the user property protocol mapper. This custom logic
 * includes the checking of roles in user attributes and then adding the user property to the claim.
 */
public class RoleBasedUserInfoProviderMapper extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper
{

    public static final String                        ROLES_ATTRIBUTE_NAME     = "professionalRoles";
    public static final String                        ROLES_LABEL_NAME         = "Professional roles";
    public static final String                        DEFAULT_ROLE             = "LEHR";

    public static final String                        NEGATE_OUTPUT_NAME       = "negateOutput";
    public static final String                        NEGATE_OUTPUT_LABEL_NAME = "Negate output";

    private static final String                       CATEGORY                 = "Role based user property mapper";
    public static final String                        PROVIDER_ID              = "roles-based-user-info-provider-mapper";

    private static final List<ProviderConfigProperty> configProperties         = new ArrayList<ProviderConfigProperty>();
    private static final UserInfoHelper               userInfoHelper           = new UserInfoHelper();

    static {
        ProviderConfigProperty userAttrProperty = new ProviderConfigProperty();
        userAttrProperty.setName(ProtocolMapperUtils.USER_ATTRIBUTE);
        userAttrProperty.setLabel(ProtocolMapperUtils.USER_MODEL_PROPERTY_LABEL);
        userAttrProperty.setType(ProviderConfigProperty.STRING_TYPE);
        userAttrProperty.setHelpText(ProtocolMapperUtils.USER_MODEL_PROPERTY_HELP_TEXT);
        configProperties.add(userAttrProperty);

        ProviderConfigProperty rolesProperty = new ProviderConfigProperty();
        rolesProperty.setName(ROLES_ATTRIBUTE_NAME);
        rolesProperty.setLabel(ROLES_LABEL_NAME);
        rolesProperty.setDefaultValue(DEFAULT_ROLE);
        rolesProperty.setType(ProviderConfigProperty.STRING_TYPE);
        rolesProperty.setHelpText("Includes comma separated list of roles which needs to be checked in the user attributes");
        configProperties.add(rolesProperty);

        ProviderConfigProperty negateOutput = new ProviderConfigProperty();
        negateOutput.setName(NEGATE_OUTPUT_NAME);
        negateOutput.setLabel(NEGATE_OUTPUT_LABEL_NAME);
        negateOutput.setDefaultValue(false);
        negateOutput.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negateOutput
            .setHelpText("Apply a NOT to the check result of roles. When this is true, then the condition will evaluate to true just if user does NOT have the specified role present in the user attributes. When this is false, the condition will evaluate to true just if user has the specified role");
        configProperties.add(negateOutput);

        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, RoleBasedUserInfoProviderMapper.class);
    }

    @Override
    public String getDisplayCategory()
    {
        return CATEGORY;
    }

    @Override
    public String getDisplayType()
    {
        return getDisplayCategory();
    }

    @Override
    public String getHelpText()
    {
        return "Adds user property to the token based on roles present in user attributes";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        return configProperties;
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx)
    {
        UserModel user = userSession.getUser();
        String rolesToCheck = mappingModel.getConfig().get(ROLES_ATTRIBUTE_NAME);
        boolean negateOutput = Boolean.parseBoolean(mappingModel.getConfig().get(NEGATE_OUTPUT_NAME));
        boolean roleInUser = userInfoHelper.checkUserAttributeRoles(rolesToCheck, Collections.singletonList("rolle"), user, negateOutput);
        if (roleInUser) {
            String propertyName = mappingModel.getConfig().get(ProtocolMapperUtils.USER_ATTRIBUTE);

            if (propertyName == null || propertyName.trim().isEmpty())
                return;

            String propertyValue = ProtocolMapperUtils.getUserModelValue(user, propertyName);
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, propertyValue);
        }
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer,
                               ProtocolMapperModel mapperModel)
        throws ProtocolMapperConfigException
    {
        String professionalRoles = mapperModel.getConfig().get(ROLES_ATTRIBUTE_NAME);

        if (professionalRoles == null || professionalRoles.trim().isEmpty()) {
            throw new ProtocolMapperConfigException("", "Professional roles field cannot be empty", "");
        }
    }
}
