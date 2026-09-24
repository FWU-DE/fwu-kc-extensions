package de.intension.mapper.oidc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.intension.api.UserInfoAttribute;
import de.intension.api.enumerations.Rolle;
import de.intension.api.json.UserInfo;
import de.intension.mapper.user.RandomUserInfoFiller;
import de.intension.mapper.user.UserInfoHelper;

/**
 * Test-only mapper that produces a randomized, but stable-per-user, userInfo claim.
 * Real values already present as user attributes (mapped exactly like {@link UserInfoProviderMapper} does via
 * {@link UserInfoHelper}) are kept; everything else is fabricated deterministically from the user's id, so the
 * same user gets the same fake data on every login as long as their underlying attributes don't change.
 * <p>
 * The claim is only added if the user has a role configured via a person.kontext(.[#]).rolle attribute; the
 * resulting userInfo object always contains exactly one personenkontext, carrying that role.
 */
public class RandomUserInfoProviderMapper extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper
{

    public static final String                        PROVIDER_ID              = "vidis-random-info-provider-mapper";
    public static final String                        USER_INFO_ATTRIBUTE_NAME = "vidisInfo";
    protected static final Logger                     logger                   = Logger.getLogger(RandomUserInfoProviderMapper.class);
    private static final String                       CATEGORY                 = "Vidis Info Mapper (Test)";

    private static final List<ProviderConfigProperty> configProperties         = new ArrayList<>();

    private static final UserInfoHelper               userInfoHelper           = new UserInfoHelper();
    private static final RandomUserInfoFiller          randomUserInfoFiller     = new RandomUserInfoFiller();
    private static final ProtocolMapperModel           allFieldsActiveModel     = buildAllFieldsActiveModel();

    static {
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, RandomUserInfoProviderMapper.class);
        setDefaultTokenClaimNameValue();
        setDefaultTokenClaimType();
        deactivateAccessToken();
    }

    /**
     * Build a mapper config that reports every {@link UserInfoAttribute} as active, so
     * {@link UserInfoHelper#getUserInfoFromKeycloakUser} maps every real user attribute it can find.
     */
    private static ProtocolMapperModel buildAllFieldsActiveModel()
    {
        ProtocolMapperModel model = new ProtocolMapperModel();
        Map<String, String> config = new HashMap<>();
        for (UserInfoAttribute attribute : UserInfoAttribute.values()) {
            config.put(attribute.getAttributeName(), "true");
        }
        model.setConfig(config);
        return model;
    }

    /**
     * Set default for field "claim.name".
     */
    private static void setDefaultTokenClaimNameValue()
    {
        Optional<ProviderConfigProperty> config = configProperties.stream().filter(p -> p.getName().equals(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))
            .findFirst();
        config.ifPresent(providerConfigProperty -> providerConfigProperty.setDefaultValue(USER_INFO_ATTRIBUTE_NAME));
    }

    /**
     * Set default for field "jsonType.label".
     */
    private static void setDefaultTokenClaimType()
    {
        Optional<ProviderConfigProperty> config = configProperties.stream().filter(p -> p.getName().equals(OIDCAttributeMapperHelper.JSON_TYPE))
            .findFirst();
        config.ifPresent(providerConfigProperty -> providerConfigProperty.setDefaultValue("JSON"));
    }

    /**
     * Deactivate ACCESS_TOKEN as a default target storing users metadata.
     */
    private static void deactivateAccessToken()
    {
        Optional<ProviderConfigProperty> config = configProperties.stream().filter(p -> p.getName().equals(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN))
            .findFirst();
        config.ifPresent(providerConfigProperty -> providerConfigProperty.setDefaultValue("false"));
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx)
    {
        UserModel user = userSession.getUser();
        Rolle rolle = userInfoHelper.findRolle(user);
        if (rolle == null) {
            return;
        }
        UserInfo realUserInfo = userInfoHelper.getUserInfoFromKeycloakUser(keycloakSession, userSession, token, allFieldsActiveModel);
        UserInfo userInfo = randomUserInfoFiller.fill(realUserInfo, rolle, user);
        try {
            OIDCAttributeMapperHelper.mapClaim(token, mappingModel, userInfo.getJsonRepresentation());
        } catch (JsonProcessingException e) {
            logger.error("Error while creating random userInfo claim", e);
        }
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
    public String getDisplayType()
    {
        return getDisplayCategory();
    }

    @Override
    public String getDisplayCategory()
    {
        return CATEGORY;
    }

    @Override
    public String getHelpText()
    {
        return "Adds a randomly generated userInfo field to the Token. Only for testing - real user attributes are used where present, everything else is made up but stable per user.";
    }

    @Override
    public int getPriority()
    {
        return 100;
    }
}
