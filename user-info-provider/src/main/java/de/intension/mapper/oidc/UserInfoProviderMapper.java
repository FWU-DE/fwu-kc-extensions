package de.intension.mapper.oidc;

import static de.intension.api.UserInfoAttribute.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.intension.api.UserInfoAttribute;
import de.intension.api.json.UserInfo;
import de.intension.mapper.user.UserInfoHelper;

public class UserInfoProviderMapper extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper

{

    public static final String                        PROVIDER_ID              = "vidis-info-provider-mapper";
    public static final String                        USER_INFO_ATTRIBUTE_NAME = "vidisInfo";
    protected static final Logger                     logger                   = Logger.getLogger(UserInfoProviderMapper.class);
    private static final String                       CATEGORY                 = "Vidis Info Mapper";
    private static final String                       ONLY_CHILDREN_ATTR_NAME  = "childrenOnly";

    private static final List<ProviderConfigProperty> configProperties         = new ArrayList<>();

    private static final UserInfoHelper               userInfoHelper           = new UserInfoHelper();

    static {
        addConfigEntry(HEIMATORGANISATION_NAME);
        addConfigEntry(HEIMATORGANISATION_BUNDESLAND);
        addConfigEntry(PERSON_FAMILIENNAME);
        addConfigEntry(PERSON_FAMILIENNAME_INITIALEN);
        addConfigEntry(PERSON_VORNAME);
        addConfigEntry(PERSON_VORNAME_INITIALEN);
        addConfigEntry(PERSON_AKRONYM);
        addConfigEntry(PERSON_GEBURTSDATUM);
        addConfigEntry(PERSON_ALTER);
        addConfigEntry(PERSON_GESCHLECHT);
        addConfigEntry(PERSON_LOKALISIERUNG);
        addConfigEntry(PERSON_VERTRAUENSSTUFE);
        addConfigEntry(PERSON_KONTEXT_ORG_VIDIS_ID);
        addConfigEntry(PERSON_KONTEXT_ORG_KENNUNG);
        addConfigEntry(PERSON_KONTEXT_ORG_NAME);
        addConfigEntry(PERSON_KONTEXT_ORG_TYP);
        addConfigEntry(PERSON_KONTEXT_ROLLE);
        addConfigEntry(PERSON_KONTEXT_STATUS);
        addOnlyChildrenClaims();
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, UserInfoProviderMapper.class);
        setDefaultTokenClaimNameValue();
        setDefaultTokenClaimType();
        deactivateAccessToken();
    }

    /**
     * Set default for field "claim.name".
     */
    private static void setDefaultTokenClaimNameValue()
    {
        Optional<ProviderConfigProperty> config = configProperties.stream().filter(p -> p.getName().equals(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))
            .findFirst();
        if (!config.isEmpty()) {
            config.get().setDefaultValue(USER_INFO_ATTRIBUTE_NAME);
        }
    }

    /**
     * Set default for field "jsonType.label".
     */
    private static void setDefaultTokenClaimType()
    {
        Optional<ProviderConfigProperty> config = configProperties.stream().filter(p -> p.getName().equals(OIDCAttributeMapperHelper.JSON_TYPE))
            .findFirst();
        if (!config.isEmpty()) {
            config.get().setDefaultValue("JSON");
        }
    }

    /**
     * Deactivate ACCESS_TOKEN as a default target storing users metadata.
     */
    private static void deactivateAccessToken()
    {
        Optional<ProviderConfigProperty> config = configProperties.stream().filter(p -> p.getName().equals(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN))
            .findFirst();
        if (!config.isEmpty()) {
            config.get().setDefaultValue("false");
        }
    }

    /**
     * Add custom configuration entries.
     */
    private static void addConfigEntry(UserInfoAttribute attribute)
    {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(attribute.getAttributeName());
        property.setLabel(attribute.getLabel());
        property.setDefaultValue(attribute.isEnabled().toString());
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText(attribute.getHelpText());
        configProperties.add(property);
    }

    private static void addOnlyChildrenClaims()
    {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(ONLY_CHILDREN_ATTR_NAME);
        property.setLabel("Add only child attributes");
        property.setDefaultValue("false");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setHelpText("Erzeugt eine flache Struktur mit mehreren Claims im Token.");
        configProperties.add(property);
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx)
    {
        UserInfo userInfo = userInfoHelper.getUserInfoFromKeycloakUser(keycloakSession, userSession, token, mappingModel);
        if (!userInfo.isEmpty()) {
            String onlyChildren = mappingModel.getConfig().get(ONLY_CHILDREN_ATTR_NAME);
            try {
                if (!Boolean.parseBoolean(onlyChildren)) {
                    OIDCAttributeMapperHelper.mapClaim(token, mappingModel, userInfo.getJsonRepresentation());
                }
                else {
                    for (Map.Entry<ProtocolMapperModel, String> entry : userInfo
                        .getChildJsonRepresentations(OIDCAttributeMapperHelper.includeInAccessToken(mappingModel),
                                                     OIDCAttributeMapperHelper.includeInIDToken(mappingModel),
                                                     OIDCAttributeMapperHelper.includeInUserInfo(mappingModel))
                        .entrySet()) {
                        OIDCAttributeMapperHelper.mapClaim(token, entry.getKey(), entry.getValue());
                    }
                }
            } catch (JsonProcessingException e) {
                logger.error("Error while creating userInfo claim", e);
            }
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
        return "Adds userInfo field to the Token";
    }

    @Override
    public int getPriority()
    {
        return 100;
    }
}
