package de.intension.mapper.oidc;

import static de.intension.api.UserInfoAttribute.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.intension.api.UserInfoAttribute;
import de.intension.api.enumerations.*;
import de.intension.api.json.*;

public class UserInfoProviderMapper
        extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper
{

    public static final String                        PROVIDER_ID              = "user-info-provider-mapper";
    public static final String                        USER_INFO_ATTRIBUTE_NAME = "userInfo";
    protected static final Logger                     logger                   = Logger.getLogger(UserInfoProviderMapper.class);
    private static final String                       CATEGORY                 = "User Info Mapper";
    private static final List<ProviderConfigProperty> configProperties         = new ArrayList<>();

    static {
        addConfigEntry(HEIMATORGANISATION_NAME);
        addConfigEntry(HEIMATORGANISATION_BUNDESLAND);
        addConfigEntry(PERSON_FAMILIENNAME);
        addConfigEntry(PERSON_VORNAME);
        addConfigEntry(PERSON_AKRONYM);
        addConfigEntry(PERSON_GEBURTSDATUM);
        addConfigEntry(PERSON_GESCHLECHT);
        addConfigEntry(PERSON_LOKALISIERUNG);
        addConfigEntry(PERSON_VERTRAUENSSTUFE);
        addConfigEntry(PERSON_KONTEXT_ORG_KENNUNG);
        addConfigEntry(PERSON_KONTEXT_ORG_NAME);
        addConfigEntry(PERSON_KONTEXT_ORG_TYP);
        addConfigEntry(PERSON_KONTEXT_ROLLE);
        addConfigEntry(PERSON_KONTEXT_STATUS);
        OIDCAttributeMapperHelper.addAttributeConfig(configProperties, UserInfoProviderMapper.class);
        setDefaultTokeClaimNameValue();
    }

    /**
     * Set default for field "claim.name".
     */
    private static void setDefaultTokeClaimNameValue()
    {
        Optional<ProviderConfigProperty> config = configProperties.stream().filter(p -> p.getName().equals(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME))
            .findFirst();
        if (!config.isEmpty()) {
            config.get().setDefaultValue(USER_INFO_ATTRIBUTE_NAME);
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
        property.setDefaultValue(attribute.isEnabled());
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        configProperties.add(property);
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession,
                            ClientSessionContext clientSessionCtx)
    {
        UserModel user = userSession.getUser();
        UserInfo userInfo = getUserInfoFromKeycloakUser(token, mappingModel, user);
        if (!userInfo.isEmpty()) {
            try {
                OIDCAttributeMapperHelper.mapClaim(token, mappingModel, UserInfo.getJsonRepresentation(userInfo));
            } catch (JsonProcessingException e) {
                logger.error("Error while creating userInfo claim", e);
            }
        }
    }

    /**
     * Create userInfo attribute from users attributes.
     */
    private UserInfo getUserInfoFromKeycloakUser(IDToken token, ProtocolMapperModel mappingModel, UserModel user)
    {
        UserInfo userInfo = new UserInfo();
        userInfo.setPid(token.getSubject());
        addHeimatOrganisation(userInfo, mappingModel, user);
        addPerson(userInfo, mappingModel, user);
        addDefaultPersonKontext(userInfo, mappingModel, user);
        addPersonenKontextArray(userInfo, mappingModel, user);
        return userInfo;
    }

    /**
     * Add {@link Personenkontext} json structure to userInfo claim.
     */
    private void addDefaultPersonKontext(UserInfo userInfo, ProtocolMapperModel mappingModel, UserModel user)
    {
        Personenkontext kontext = new Personenkontext();
        //TODO generate hash for kitd
        kontext.setKtid(UUID.randomUUID().toString());
        Organisation organisation = new Organisation();
        //TODO generate hash for orgId
        organisation.setOrgid(UUID.randomUUID().toString());
        if (isActive(PERSON_KONTEXT_ORG_KENNUNG, mappingModel)) {
            organisation.setKennung(resolveSingleAttributeValue(user, PERSON_KONTEXT_ORG_KENNUNG));
        }
        if (isActive(PERSON_KONTEXT_ORG_NAME, mappingModel)) {
            organisation.setName(resolveSingleAttributeValue(user, PERSON_KONTEXT_ORG_NAME));
        }
        if (isActive(PERSON_KONTEXT_ORG_TYP, mappingModel)) {
            String orgTyp = resolveSingleAttributeValue(user, PERSON_KONTEXT_ORG_TYP);
            if (orgTyp != null) {
                try {
                    organisation.setTyp(OrganisationsTyp.valueOf(orgTyp));
                } catch (IllegalArgumentException e) {
                    logger.errorf("Unsupported value %s for %s", orgTyp, PERSON_KONTEXT_ORG_TYP.getAttributeName());
                }
            }
        }
        if (isActive(PERSON_KONTEXT_ROLLE, mappingModel)) {
            String rolle = resolveSingleAttributeValue(user, PERSON_KONTEXT_ROLLE);
            if (rolle != null) {
                try {
                    kontext.setRolle(Rolle.valueOf(rolle));
                } catch (IllegalArgumentException e) {
                    logger.errorf("Unsupported value %s for %s", rolle, PERSON_KONTEXT_ROLLE.getAttributeName());
                }
            }
        }
        if (isActive(PERSON_KONTEXT_STATUS, mappingModel)) {
            String status = resolveSingleAttributeValue(user, PERSON_KONTEXT_STATUS);
            if (status != null) {
                try {
                    kontext.setPersonenstatus(PersonenStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    logger.errorf("Unsupported value %s for %s", status, PERSON_KONTEXT_STATUS.getAttributeName());
                }
            }
        }
        if (!organisation.isEmpty()) {
            kontext.setOrganisation(organisation);
        }
        if (!kontext.isEmpty()) {
            userInfo.getPersonenKontexte().add(kontext);
        }
    }

    /**
     * Add @{@link HeimatOrganisation} json structure to userInfo claim.
     */
    private void addHeimatOrganisation(UserInfo userInfo, ProtocolMapperModel mappingModel, UserModel user)
    {
        HeimatOrganisation heimatOrganisation = new HeimatOrganisation();
        if (isActive(HEIMATORGANISATION_NAME, mappingModel)) {
            heimatOrganisation.setName(resolveSingleAttributeValue(user, HEIMATORGANISATION_NAME));
        }
        if (isActive(HEIMATORGANISATION_BUNDESLAND, mappingModel)) {
            heimatOrganisation.setBundesland(resolveSingleAttributeValue(user, HEIMATORGANISATION_BUNDESLAND));
        }
        if (!heimatOrganisation.isEmpty()) {
            userInfo.setHeimatOrganisation(heimatOrganisation);
        }
    }

    /**
     * Add {@link Person} json structure to userInfo claim.
     */
    private void addPerson(UserInfo userInfo, ProtocolMapperModel mappingModel, UserModel user)
    {
        Person person = new Person();
        PersonName personName = new PersonName();
        if (isActive(PERSON_FAMILIENNAME, mappingModel)) {
            personName.setFamilienname(resolveSingleAttributeValue(user, PERSON_FAMILIENNAME));
        }
        if (isActive(PERSON_VORNAME, mappingModel)) {
            personName.setVorname(resolveSingleAttributeValue(user, PERSON_VORNAME));
        }
        if (isActive(PERSON_AKRONYM, mappingModel)) {
            String akronym = resolveSingleAttributeValue(user, PERSON_AKRONYM);
            personName.setAkronym(akronym);
        }
        if (isActive(PERSON_GEBURTSDATUM, mappingModel)) {
            String geburtsdatum = resolveSingleAttributeValue(user, PERSON_GEBURTSDATUM);
            if (StringUtil.isNotBlank(geburtsdatum)) {
                Geburt geburt = new Geburt(geburtsdatum);
                person.setGeburt(geburt);
            }
        }
        if (isActive(PERSON_GESCHLECHT, mappingModel)) {
            String geschlecht = resolveSingleAttributeValue(user, PERSON_GESCHLECHT);
            if (geschlecht != null) {
                try {
                    person.setGeschlecht(Geschlecht.valueOf(geschlecht));
                } catch (IllegalArgumentException e) {
                    logger.errorf("Unsupported value %s for %s", geschlecht, PERSON_GESCHLECHT.getAttributeName());
                }
            }

        }
        if (isActive(PERSON_LOKALISIERUNG, mappingModel)) {
            person.setLokalisierung(resolveSingleAttributeValue(user, PERSON_LOKALISIERUNG));
        }
        if (isActive(PERSON_VERTRAUENSSTUFE, mappingModel)) {
            String vertrauensstufe = resolveSingleAttributeValue(user, PERSON_VERTRAUENSSTUFE);
            if (vertrauensstufe != null) {
                try {
                    person.setVertrauensstufe(Vertrauensstufe.valueOf(vertrauensstufe));
                } catch (IllegalArgumentException e) {
                    logger.errorf("Unsupported value %s for %s", vertrauensstufe, PERSON_VERTRAUENSSTUFE.getAttributeName());
                }
            }
        }
        if (!personName.isEmpty()) {
            person.setPerson(personName);
        }
        if (!person.isEmpty()) {
            userInfo.setPerson(person);
        }
    }

    /**
     * Add personenkontext arrays.
     */
    private void addPersonenKontextArray(UserInfo userInfo, ProtocolMapperModel mappingModel, UserModel user)
    {
        Set<Integer> indizes = getPersonenKontexteIndizes(user);
        for (Integer i : indizes) {
            Personenkontext kontext = new Personenkontext();
            //TODO generate hash for kitd
            kontext.setKtid(UUID.randomUUID().toString());
            Organisation organisation = new Organisation();
            //TODO generate hash for orgId
            organisation.setOrgid(UUID.randomUUID().toString());
            if (isActive(PERSON_KONTEXT_ORG_KENNUNG, mappingModel)) {
                organisation.setKennung(resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_ORG_KENNUNG, i));
            }
            if (isActive(PERSON_KONTEXT_ORG_NAME, mappingModel)) {
                organisation.setName(resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_ORG_NAME, i));
            }
            if (isActive(PERSON_KONTEXT_ORG_TYP, mappingModel)) {
                String orgTyp = resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_ORG_TYP, i);
                if (orgTyp != null) {
                    try {
                        organisation.setTyp(OrganisationsTyp.valueOf(orgTyp));
                    } catch (IllegalArgumentException e) {
                        logger.errorf("Unsupported value %s for %s", orgTyp, PERSON_KONTEXT_ORG_TYP.getAttributeName());
                    }
                }
            }
            if (isActive(PERSON_KONTEXT_ROLLE, mappingModel)) {
                String rolle = resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_ROLLE, i);
                if (rolle != null) {
                    try {
                        kontext.setRolle(Rolle.valueOf(rolle));
                    } catch (IllegalArgumentException e) {
                        logger.errorf("Unsupported value %s for %s", rolle, PERSON_KONTEXT_ROLLE.getAttributeName());
                    }
                }

            }
            if (isActive(PERSON_KONTEXT_STATUS, mappingModel)) {
                String status = resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_STATUS, i);
                if (status != null) {
                    try {
                        kontext.setPersonenstatus(PersonenStatus.valueOf(status));
                    } catch (IllegalArgumentException e) {
                        logger.errorf("Unsupported value %s for %s", status, PERSON_KONTEXT_STATUS.getAttributeName());
                    }
                }
            }
            if (!organisation.isEmpty()) {
                kontext.setOrganisation(organisation);
            }
            if (!kontext.isEmpty()) {
                userInfo.getPersonenKontexte().add(kontext);
            }
        }
    }

    /**
     * Get all personenkontext indices.
     */
    private Set<Integer> getPersonenKontexteIndizes(UserModel user)
    {
        Set<Integer> indices = new HashSet<>();
        Map<String, List<String>> attributes = user.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            Pattern pattern = Pattern.compile("^person\\.kontext\\[(\\d+)]\\..*");
            for (String attributeKey : attributes.keySet()) {
                Matcher matcher = pattern.matcher(attributeKey);
                if (matcher.matches()) {
                    String index = matcher.group(1);
                    indices.add(Integer.valueOf(index));
                }
            }
        }
        return indices;
    }

    /**
     * Resolve single user attribute value.
     */
    private String resolveSingleAttributeValue(UserModel user, UserInfoAttribute attribute)
    {
        return resolveSingleAttributeValue(user, attribute, -1);
    }

    /**
     * Resolve single user attribute value with array support (index >= 0).
     */
    private String resolveSingleAttributeValue(UserModel user, UserInfoAttribute attribute, int index)
    {
        Collection<String> values;
        if (index == -1) {
            values = KeycloakModelUtils.resolveAttribute(user, attribute.getAttributeName(), false);
        }
        else {
            values = KeycloakModelUtils.resolveAttribute(user, attribute.getAttributeName().replace("#", String.valueOf(index)), false);
        }
        if (!values.isEmpty()) {
            return values.stream().iterator().next();
        }
        else if (attribute.getDefaultValue() != null) {
            return attribute.getDefaultValue().toString();
        }
        return null;
    }

    /**
     * Is user attribute send to Service Provider by default.
     */
    private boolean isActive(UserInfoAttribute userInfoAttribute, ProtocolMapperModel mappingModel)
    {
        String value = mappingModel.getConfig().get(userInfoAttribute.getAttributeName());
        return Boolean.valueOf(value);
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
