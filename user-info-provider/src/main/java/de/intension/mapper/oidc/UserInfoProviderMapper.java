package de.intension.mapper.oidc;

import static de.intension.api.UserInfoAttribute.HEIMATORGANISATION_BUNDESLAND;
import static de.intension.api.UserInfoAttribute.HEIMATORGANISATION_NAME;
import static de.intension.api.UserInfoAttribute.PERSON_AKRONYM;
import static de.intension.api.UserInfoAttribute.PERSON_ALTER;
import static de.intension.api.UserInfoAttribute.PERSON_FAMILIENNAME;
import static de.intension.api.UserInfoAttribute.PERSON_FAMILIENNAME_INITIALEN;
import static de.intension.api.UserInfoAttribute.PERSON_GEBURTSDATUM;
import static de.intension.api.UserInfoAttribute.PERSON_GEBURTSORT;
import static de.intension.api.UserInfoAttribute.PERSON_GESCHLECHT;
import static de.intension.api.UserInfoAttribute.PERSON_KONTEXT_GRUPPEN;
import static de.intension.api.UserInfoAttribute.PERSON_KONTEXT_LOESCHUNG;
import static de.intension.api.UserInfoAttribute.PERSON_KONTEXT_ORG_KENNUNG;
import static de.intension.api.UserInfoAttribute.PERSON_KONTEXT_ORG_NAME;
import static de.intension.api.UserInfoAttribute.PERSON_KONTEXT_ORG_TYP;
import static de.intension.api.UserInfoAttribute.PERSON_KONTEXT_ORG_VIDIS_ID;
import static de.intension.api.UserInfoAttribute.PERSON_KONTEXT_ROLLE;
import static de.intension.api.UserInfoAttribute.PERSON_KONTEXT_STATUS;
import static de.intension.api.UserInfoAttribute.PERSON_LOKALISIERUNG;
import static de.intension.api.UserInfoAttribute.PERSON_REFERRER;
import static de.intension.api.UserInfoAttribute.PERSON_VERTRAUENSSTUFE;
import static de.intension.api.UserInfoAttribute.PERSON_VOLLJAEHRIG;
import static de.intension.api.UserInfoAttribute.PERSON_VORNAME;
import static de.intension.api.UserInfoAttribute.PERSON_VORNAME_INITIALEN;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
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
    private static final String                       PROFESSIONAL_ROLES       = "professionalRoles";
    public static final String                        NEGATE_OUTPUT_NAME       = "negateOutput";
    public static final String                        NEGATE_OUTPUT_LABEL_NAME = "Negate output";

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
        addConfigEntry(PERSON_GEBURTSORT);
        addConfigEntry(PERSON_VOLLJAEHRIG);
        addConfigEntry(PERSON_GESCHLECHT);
        addConfigEntry(PERSON_LOKALISIERUNG);
        addConfigEntry(PERSON_VERTRAUENSSTUFE);
        addConfigEntry(PERSON_REFERRER);
        addConfigEntry(PERSON_KONTEXT_ORG_VIDIS_ID);
        addConfigEntry(PERSON_KONTEXT_ORG_KENNUNG);
        addConfigEntry(PERSON_KONTEXT_ORG_NAME);
        addConfigEntry(PERSON_KONTEXT_ORG_TYP);
        addConfigEntry(PERSON_KONTEXT_ROLLE);
        addConfigEntry(PERSON_KONTEXT_STATUS);
        addConfigEntry(PERSON_KONTEXT_GRUPPEN);
        addConfigEntry(PERSON_KONTEXT_LOESCHUNG);
        addOnlyChildrenClaims();
        addAttributesBasedOnRole();
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

    private static void addAttributesBasedOnRole()
    {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(PROFESSIONAL_ROLES);
        property.setLabel("Professional roles");
        property.setDefaultValue("LEHR");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setHelpText("Roles based on which attributes should be mapped");
        configProperties.add(property);

        ProviderConfigProperty negateOutput = new ProviderConfigProperty();
        negateOutput.setName(NEGATE_OUTPUT_NAME);
        negateOutput.setLabel(NEGATE_OUTPUT_LABEL_NAME);
        negateOutput.setDefaultValue(false);
        negateOutput.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        negateOutput
            .setHelpText("Apply a NOT to the check result of roles. When this is true, then the condition will evaluate to true just if user does NOT have the specified role present in the user attributes. When this is false, the condition will evaluate to true just if user has the specified role");
        configProperties.add(negateOutput);
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx)
    {
        UserInfo userInfo = userInfoHelper.getUserInfoFromKeycloakUser(keycloakSession, userSession, token, mappingModel);
        if (!userInfo.isEmpty()) {
            String onlyChildren = mappingModel.getConfig().get(ONLY_CHILDREN_ATTR_NAME);

            String rolesToCheck = mappingModel.getConfig().get(PROFESSIONAL_ROLES);
            boolean negateOutput = Boolean.parseBoolean(mappingModel.getConfig().get(NEGATE_OUTPUT_NAME));
            if (rolesToCheck != null && !rolesToCheck.trim().isEmpty()) {
                boolean roleInUser = userInfoHelper.checkUserAttributeRoles(rolesToCheck, null, userSession.getUser(), negateOutput);
                if (!roleInUser) {
                    userInfo.removePersonNameTag();
                }
            }

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
