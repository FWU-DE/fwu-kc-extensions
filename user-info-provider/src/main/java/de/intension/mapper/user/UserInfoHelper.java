package de.intension.mapper.user;

import static de.intension.api.UserInfoAttribute.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.StringUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.hash.Hashing;

import de.intension.api.UserInfoAttribute;
import de.intension.api.enumerations.*;
import de.intension.api.json.*;

public class UserInfoHelper
{

    protected static final Logger                  logger                = Logger.getLogger(UserInfoHelper.class);
    private static final String                    LOG_UNSUPPORTED_VALUE = "Unsupported value %s for %s";
    private static final UserBirthdayHelper        birthdayHelper        = new UserBirthdayHelper();
    private static final UserVolljaehrigkeitHelper volljaehrigkeitHelper = new UserVolljaehrigkeitHelper();
    private static final IdpHelper                 idpHelper             = new IdpHelper();
    private static final ObjectMapper              objectMapper          = new ObjectMapper();
    private static final String                    INDEXED_ATTR_FORMAT   = "%s[%d]";

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Create userInfo attribute from users attributes.
     */
    public UserInfo getUserInfoFromKeycloakUser(KeycloakSession keycloakSession, UserSessionModel session, IDToken token, ProtocolMapperModel mappingModel)
    {
        UserInfo userInfo = new UserInfo();
        userInfo.setPid(getSubject(token, session));
        String heimatOrgId = addHeimatOrganisation(keycloakSession, session, userInfo, mappingModel);
        addPerson(userInfo, mappingModel, session.getUser());
        addDefaultPersonKontext(userInfo, mappingModel, session.getUser(), heimatOrgId);
        addPersonenKontextArray(userInfo, mappingModel, session.getUser(), heimatOrgId);
        return userInfo;
    }

    /**
     * Get subject identifier from token.
     */
    private String getSubject(IDToken token, UserSessionModel sessionModel)
    {
        String subject = token.getSubject();
        if (StringUtil.isBlank(subject)) {
            Map<String, Object> otherClaims = token.getOtherClaims();
            if (otherClaims != null) {
                subject = (String)otherClaims.get("sub");
            }
            //Fallback - Use User-ID from Keycloak
            if (StringUtil.isBlank(subject) && sessionModel.getUser() != null) {
                subject = sessionModel.getUser().getId();
            }
        }
        return subject;
    }

    /**
     * Add {@link Personenkontext} json structure to userInfo claim.
     */
    private void addDefaultPersonKontext(UserInfo userInfo, ProtocolMapperModel mappingModel, UserModel user, String heimatOrgId)
    {
        Personenkontext kontext = new Personenkontext();
        Rolle rolle = getRolle(user, -1);
        kontext.setId(getKit(user, heimatOrgId, rolle, -1));
        if (isActive(PERSON_KONTEXT_ROLLE, mappingModel)) {
            kontext.setRolle(rolle);
        }
        if (isActive(PERSON_KONTEXT_STATUS, mappingModel)) {
            String status = resolveSingleAttributeValue(user, PERSON_KONTEXT_STATUS);
            if (status != null) {
                try {
                    kontext.setPersonenstatus(PersonenStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    logger.errorf(LOG_UNSUPPORTED_VALUE, status, PERSON_KONTEXT_STATUS.getAttributeName());
                }
            }
        }
        Organisation organisation = getOrganisation(mappingModel, user, heimatOrgId, rolle, userInfo);
        if (!organisation.isEmpty()) {
            kontext.setOrganisation(organisation);
        }
        if (isActive(PERSON_KONTEXT_GRUPPEN, mappingModel)) {
            addGruppenToKontext(user, kontext, PERSON_KONTEXT_GRUPPEN.getAttributeName());
        }
        if (isActive(PERSON_KONTEXT_LOESCHUNG, mappingModel)) {
            addLoeschungToPersonenkontext(user, kontext);
        }
        if (!kontext.isEmpty()) {
            userInfo.getPersonenKontexte().add(kontext);
        }
    }

    /**
     * Add {@link Organisation} json structure to userInfo claim.
     */
    private Organisation getOrganisation(ProtocolMapperModel mappingModel, UserModel user, String heimatOrgId, Rolle rolle, UserInfo userInfo)
    {
        Organisation organisation = new Organisation();
        String kennung = resolveSingleAttributeValue(user, PERSON_KONTEXT_ORG_KENNUNG);
        organisation.setOrgid(getOrgId(user, heimatOrgId, rolle, kennung, -1));
        if (isActive(PERSON_KONTEXT_ORG_NAME, mappingModel)) {
            organisation.setName(resolveSingleAttributeValue(user, PERSON_KONTEXT_ORG_NAME));
        }
        if (isActive(PERSON_KONTEXT_ORG_KENNUNG, mappingModel)) {
            organisation.setKennung(kennung);
        }
        if (isActive(PERSON_KONTEXT_ORG_TYP, mappingModel)) {
            String orgTyp = resolveSingleAttributeValue(user, PERSON_KONTEXT_ORG_TYP);
            if (orgTyp != null) {
                try {
                    organisation.setTyp(OrganisationsTyp.valueOf(orgTyp));
                } catch (IllegalArgumentException e) {
                    logger.errorf(LOG_UNSUPPORTED_VALUE, orgTyp, PERSON_KONTEXT_ORG_TYP.getAttributeName());
                }
            }
        }
        if (isActive(PERSON_KONTEXT_ORG_VIDIS_ID, mappingModel)) {
            addVidisSchulIdentifikator(mappingModel, user, userInfo, organisation, kennung, -1);
        }
        return organisation;
    }

    /**
     * Add @{@link HeimatOrganisation} json structure to userInfo claim.
     */
    private String addHeimatOrganisation(KeycloakSession keycloakSession, UserSessionModel session, UserInfo userInfo, ProtocolMapperModel mappingModel)
    {
        HeimatOrganisation heimatOrganisation = new HeimatOrganisation();
        IdentityProviderModel idpProviderModel = idpHelper.getIdpAlias(keycloakSession, session);
        if (idpProviderModel != null) {
            heimatOrganisation.setId(idpProviderModel.getAlias());
            if (isActive(HEIMATORGANISATION_NAME, mappingModel)) {
                heimatOrganisation.setName(idpProviderModel.getDisplayName());
            }
        }
        if (isActive(HEIMATORGANISATION_BUNDESLAND, mappingModel)) {
            heimatOrganisation.setBundesland(resolveSingleAttributeValue(session.getUser(), HEIMATORGANISATION_BUNDESLAND));
        }
        if (!heimatOrganisation.isEmpty()) {
            userInfo.setHeimatOrganisation(heimatOrganisation);
        }
        return heimatOrganisation.getId();
    }

    /**
     * Add {@link Person} json structure to userInfo claim.
     */
    private void addPerson(UserInfo userInfo, ProtocolMapperModel mappingModel, UserModel user)
    {
        Person person = new Person();
        addPersonName(person, mappingModel, user);
        addGeschlecht(person, mappingModel, user);
        addGeburt(person, mappingModel, user);
        if (isActive(PERSON_LOKALISIERUNG, mappingModel)) {
            person.setLokalisierung(resolveSingleAttributeValue(user, PERSON_LOKALISIERUNG));
        }
        if (isActive(PERSON_VERTRAUENSSTUFE, mappingModel)) {
            String vertrauensstufe = resolveSingleAttributeValue(user, PERSON_VERTRAUENSSTUFE);
            if (vertrauensstufe != null) {
                try {
                    person.setVertrauensstufe(Vertrauensstufe.valueOf(vertrauensstufe));
                } catch (IllegalArgumentException e) {
                    logger.errorf(LOG_UNSUPPORTED_VALUE, vertrauensstufe, PERSON_VERTRAUENSSTUFE.getAttributeName());
                }
            }
        }
        if (!person.isEmpty()) {
            userInfo.setPerson(person);
        }
    }

    private void addLoeschungToPersonenkontext(UserModel user, Personenkontext personenkontext)
    {
        addLoeschungToPersonenkontext(user, personenkontext, null);
    }

    /**
     * Add {@link PersonName} json structure to userInfo claim.
     */
    private void addPersonName(Person person, ProtocolMapperModel mappingModel, UserModel user)
    {
        PersonName personName = new PersonName();
        String familienName = getFamilienname(user);
        String vorname = getVorname(user);
        if (isActive(PERSON_FAMILIENNAME, mappingModel)) {
            personName.setFamilienname(familienName);
        }
        if (isActive(PERSON_FAMILIENNAME_INITIALEN, mappingModel)) {
            personName.setInitialenFamilienname(resolveSingleAttributeValue(user, PERSON_FAMILIENNAME_INITIALEN));
        }
        if (isActive(PERSON_VORNAME, mappingModel)) {
            personName.setVorname(vorname);
        }
        if (isActive(PERSON_VORNAME_INITIALEN, mappingModel)) {
            personName.setInitialenVorname(resolveSingleAttributeValue(user, PERSON_VORNAME_INITIALEN));
        }
        if (isActive(PERSON_AKRONYM, mappingModel)) {
            String akronym = resolveSingleAttributeValue(user, PERSON_AKRONYM);
            if (StringUtil.isBlank(akronym) && vorname != null && vorname.length() >= 2 && familienName != null && familienName.length() >= 2) {
                akronym = vorname.substring(0, 2).concat(familienName.substring(0, 2));
            }
            if (akronym != null) {
                personName.setAkronym(akronym.toLowerCase());
            }
        }
        if (!personName.isEmpty()) {
            person.setPersonName(personName);
        }
    }

    /**
     * Get first name either from user attribute or user property (fallback).
     */
    private String getVorname(UserModel user)
    {
        String vorname = resolveSingleAttributeValue(user, PERSON_VORNAME);
        if (vorname == null || vorname.isEmpty()) {
            vorname = user.getFirstName();
        }
        return vorname;
    }

    /**
     * Get last name either from user attribute or user property (fallback).
     */
    private String getFamilienname(UserModel user)
    {
        String familienname = resolveSingleAttributeValue(user, PERSON_FAMILIENNAME);
        if (familienname == null || familienname.isEmpty()) {
            familienname = user.getLastName();
        }
        return familienname;
    }

    /**
     * Add {@link Geschlecht} json structure to userInfo claim.
     */
    private void addGeschlecht(Person person, ProtocolMapperModel mappingModel, UserModel user)
    {
        if (isActive(PERSON_GESCHLECHT, mappingModel)) {
            String geschlecht = resolveSingleAttributeValue(user, PERSON_GESCHLECHT);
            if (geschlecht != null) {
                try {
                    person.setGeschlecht(Geschlecht.valueOf(geschlecht));
                } catch (IllegalArgumentException e) {
                    logger.errorf(LOG_UNSUPPORTED_VALUE, geschlecht, PERSON_GESCHLECHT.getAttributeName());
                }
            }
        }
    }

    private void addLoeschungToPersonenkontext(UserModel user, Personenkontext kontext, Integer indexOfPersonenkontext)
    {
        String loeschungJson = resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_LOESCHUNG, indexOfPersonenkontext);
        if (StringUtil.isNotBlank(loeschungJson)) {
            try {
                kontext.setLoeschung(objectMapper.readValue(loeschungJson, Loeschung.class));
            } catch (JsonProcessingException e) {
                logger.errorf(e, "Could not read Attribute %s from user %s", PERSON_KONTEXT_ARRAY_LOESCHUNG.getAttributeName(), user.getUsername());
            }
        }
    }

    /**
     * Resolve single user attribute value with array support (index >= 0).
     */
    private String resolveSingleAttributeValue(UserModel user, UserInfoAttribute attribute, Integer index)
    {
        Collection<String> values;
        if (index == null) {
            values = KeycloakModelUtils.resolveAttribute(user, attribute.getAttributeName(), false);
        }
        else {
            values = KeycloakModelUtils.resolveAttribute(user, getIndexedAttributeName(attribute, index), false);
        }
        if (!values.isEmpty()) {
            return values.stream().iterator().next();
        }
        else if (attribute.getDefaultValue() != null) {
            return attribute.getDefaultValue().toString();
        }
        return null;
    }

    public static String getIndexedAttributeName(UserInfoAttribute attribute, int index)
    {
        return attribute.getAttributeName().replace("#", String.valueOf(index));
    }

    private void addGruppenToKontext(UserModel user, Personenkontext kontext, String attributeName)
    {
        kontext.setGruppen(new ArrayList<>());
        int index = 0;
        String indexedAttribute = String.format(INDEXED_ATTR_FORMAT, attributeName, index);
        String json = resolveSplittedAttribute(user, indexedAttribute);
        while (StringUtil.isNotBlank(json)) {
            try {
                GruppeWithZugehoerigkeit gruppe = objectMapper.readValue(json, GruppeWithZugehoerigkeit.class);
                kontext.getGruppen().add(gruppe);
            } catch (JsonProcessingException e) {
                logger.errorf(e, "Could not deserialize person.kontext.gruppen[%d] for user %s" + index, user.getUsername());
            }
            indexedAttribute = String.format(INDEXED_ATTR_FORMAT, attributeName, index++);
            json = resolveSplittedAttribute(user, indexedAttribute);
        }
    }

    /**
     * Add personenkontext arrays.
     */
    private void addPersonenKontextArray(UserInfo userInfo, ProtocolMapperModel mappingModel, UserModel user, String heimatOrgId)
    {
        Set<Integer> indizes = getPersonenKontexteIndizes(user);
        for (Integer i : indizes) {
            Personenkontext kontext = getKontextArr(userInfo, mappingModel, user, heimatOrgId, i);
            if (!kontext.isEmpty()) {
                userInfo.getPersonenKontexte().add(kontext);
            }
        }
    }

    private String resolveSplittedAttribute(UserModel user, String attributeName)
    {
        StringBuilder jsonBuilder = new StringBuilder();
        int partialIndex = 0;
        Optional<String> partial = KeycloakModelUtils.resolveAttribute(user, attributeName + "_" + partialIndex, false).stream()
            .findFirst();
        while (partial.isPresent() && partial.get().length() == 255) {
            jsonBuilder.append(partial.get());
            partialIndex++;
            partial = KeycloakModelUtils.resolveAttribute(user, attributeName + "_" + partialIndex, false).stream().findFirst();
        }
        partial.ifPresent(jsonBuilder::append);
        return jsonBuilder.toString();
    }

    /**
     * Add {@link Organisation} json structure to userInfo claim.
     */
    private Organisation getOrganisationArray(ProtocolMapperModel mappingModel, UserModel user, String heimatOrgId, Rolle rolle, Integer i, UserInfo userInfo)
    {
        Organisation organisation = new Organisation();
        String kennung = resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_ORG_KENNUNG, i);
        organisation.setOrgid(getOrgId(user, heimatOrgId, rolle, kennung, i));
        if (isActive(PERSON_KONTEXT_ORG_NAME, mappingModel)) {
            organisation.setName(resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_ORG_NAME, i));
        }
        if (isActive(PERSON_KONTEXT_ORG_KENNUNG, mappingModel)) {
            organisation.setKennung(kennung);
        }
        if (isActive(PERSON_KONTEXT_ORG_TYP, mappingModel)) {
            String orgTyp = resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_ORG_TYP, i);
            if (orgTyp != null) {
                try {
                    organisation.setTyp(OrganisationsTyp.valueOf(orgTyp));
                } catch (IllegalArgumentException e) {
                    logger.errorf(LOG_UNSUPPORTED_VALUE, orgTyp, PERSON_KONTEXT_ORG_TYP.getAttributeName());
                }
            }
        }
        if (isActive(PERSON_KONTEXT_ORG_VIDIS_ID, mappingModel)) {
            addVidisSchulIdentifikator(mappingModel, user, userInfo, organisation, kennung, i);
        }
        return organisation;
    }

    /**
     * Add vidis schulidentifikator to Organisation
     */
    private void addVidisSchulIdentifikator(ProtocolMapperModel mappingModel, UserModel user, UserInfo userInfo, Organisation org, String kennung,
                                            Integer index)
    {
        UserInfoAttribute attribute = PERSON_KONTEXT_ORG_VIDIS_ID;
        if (index != -1) {
            attribute = PERSON_KONTEXT_ARRAY_ORG_VIDIS_ID;
        }
        if (isActive(PERSON_KONTEXT_ORG_VIDIS_ID, mappingModel)) {
            String vidisId = resolveSingleAttributeValue(user, attribute, index);
            if (StringUtil.isBlank(vidisId) && userInfo.getHeimatOrganisation() != null
                    && StringUtil.isNotBlank(userInfo.getHeimatOrganisation().getId()) && StringUtil.isNotBlank(kennung)) {
                org.setVidisSchulidentifikator(String.format("%s.%s", userInfo.getHeimatOrganisation().getId(), kennung).toLowerCase());
            }
            else if (vidisId != null) {
                org.setVidisSchulidentifikator(vidisId);
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
        return resolveSingleAttributeValue(user, attribute, null);
    }

    /**
     * Add @{@link Geburt} to @{@link Person} json structure.
     */
    private void addGeburt(Person person, ProtocolMapperModel mappingModel, UserModel user)
    {
        String geburtsdatum = resolveSingleAttributeValue(user, PERSON_GEBURTSDATUM);
        Integer age = null;
        if (birthdayHelper.isValidBirthdayFormat(geburtsdatum)) {
            Geburt geburt = new Geburt();
            if (isActive(PERSON_GEBURTSDATUM, mappingModel)) {
                geburt.setDatum(geburtsdatum);
            }
            if (isActive(PERSON_GEBURTSORT, mappingModel)) {
                geburt.setGeburtsort(resolveSingleAttributeValue(user, PERSON_GEBURTSORT));
            }
            if (isActive(PERSON_ALTER, mappingModel)) {
                age = birthdayHelper.calculateAge(geburtsdatum);
                geburt.setAlter(age);
            }
            if (isActive(PERSON_VOLLJAEHRIG, mappingModel)) {
                age = age == null ? birthdayHelper.calculateAge(geburtsdatum) : age;
                geburt.setVolljaehrig(volljaehrigkeitHelper.isVolljaehrig(age));
            }
            if (!geburt.isEmpty()) {
                person.setGeburt(geburt);
            }
        }
    }

    /**
     * Get single Kontext from array structure.
     */
    private Personenkontext getKontextArr(UserInfo userInfo, ProtocolMapperModel mappingModel, UserModel user, String heimatOrgId, Integer i)
    {
        Rolle rolle = getRolle(user, i);
        Personenkontext kontext = new Personenkontext();
        kontext.setId(getKit(user, heimatOrgId, rolle, i));
        Organisation organisation = getOrganisationArray(mappingModel, user, heimatOrgId, rolle, i, userInfo);
        if (isActive(PERSON_KONTEXT_ROLLE, mappingModel)) {
            kontext.setRolle(rolle);
        }
        if (isActive(PERSON_KONTEXT_STATUS, mappingModel)) {
            String status = resolveSingleAttributeValue(user, PERSON_KONTEXT_ARRAY_STATUS, i);
            if (status != null) {
                try {
                    kontext.setPersonenstatus(PersonenStatus.valueOf(status));
                } catch (IllegalArgumentException e) {
                    logger.errorf(LOG_UNSUPPORTED_VALUE, status, PERSON_KONTEXT_STATUS.getAttributeName());
                }
            }
        }
        if (isActive(PERSON_KONTEXT_GRUPPEN, mappingModel)) {
            addGruppenToKontext(user, kontext, getIndexedAttributeName(PERSON_KONTEXT_ARRAY_GRUPPEN, i));
        }
        if (isActive(PERSON_KONTEXT_LOESCHUNG, mappingModel)) {
            addLoeschungToPersonenkontext(user, kontext, i);
        }
        if (!organisation.isEmpty()) {
            kontext.setOrganisation(organisation);
        }
        return kontext;
    }

    /**
     * Get role from person context.
     */
    private Rolle getRolle(UserModel user, Integer index)
    {
        UserInfoAttribute attribute = PERSON_KONTEXT_ROLLE;
        if (index != -1) {
            attribute = PERSON_KONTEXT_ARRAY_ROLLE;
        }
        Rolle rolle = null;
        String sRolle = resolveSingleAttributeValue(user, attribute, index);
        if (sRolle != null) {
            try {
                rolle = Rolle.valueOf(sRolle);
            } catch (IllegalArgumentException e) {
                logger.errorf(LOG_UNSUPPORTED_VALUE, sRolle, attribute.getAttributeName());
            }
        }
        return rolle;
    }

    /**
     * Get generated context id hash.
     */
    private String getKit(UserModel user, String heimatOrgId, Rolle rolle, Integer index)
    {
        UserInfoAttribute attribute = PERSON_KONTEXT_ID;
        if (index != null) {
            attribute = PERSON_KONTEXT_ARRAY_ID;
        }
        String kontextId = resolveSingleAttributeValue(user, attribute, index);
        if (StringUtil.isBlank(kontextId) && rolle != null && StringUtil.isNotBlank(heimatOrgId)) {
            String builder = rolle.name() + heimatOrgId;
            kontextId = Hashing.sha256()
                .hashString(builder, StandardCharsets.UTF_8)
                .toString();
        }
        return kontextId;
    }

    /**
     * Get generated organisation id hash.
     */
    private String getOrgId(UserModel user, String heimatOrgId, Rolle rolle, String kennung, Integer index)
    {
        UserInfoAttribute attribute = PERSON_KONTEXT_ORG_ID;
        if (index != -1) {
            attribute = PERSON_KONTEXT_ARRAY_ORG_ID;
        }
        String orgId = resolveSingleAttributeValue(user, attribute, index);
        if ((StringUtil.isBlank(orgId)) && rolle != null && StringUtil.isNotBlank(heimatOrgId) && StringUtil.isNotBlank(kennung)) {
            String builder = rolle.name() + heimatOrgId + kennung;
            orgId = Hashing.sha256()
                .hashString(builder, StandardCharsets.UTF_8)
                .toString();
        }
        return orgId;
    }

    /**
     * Is user attribute send to Service Provider by default.
     */
    private boolean isActive(UserInfoAttribute userInfoAttribute, ProtocolMapperModel mappingModel)
    {
        String value = mappingModel.getConfig().get(userInfoAttribute.getAttributeName());
        return Boolean.parseBoolean(value);
    }

}
