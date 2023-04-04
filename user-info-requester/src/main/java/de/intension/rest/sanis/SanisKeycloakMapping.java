package de.intension.rest.sanis;

import static de.intension.api.UserInfoAttribute.*;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.UserModel;

import com.google.common.base.Splitter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.PathNotFoundException;

import de.intension.api.UserInfoAttribute;
import de.intension.rest.BaseMapper;
import de.intension.rest.GruppenMapper;
import de.intension.rest.IKeycloakApiMapper;
import de.intension.rest.IValueMapper;

public class SanisKeycloakMapping
    implements IKeycloakApiMapper
{

    protected static final Logger                                 logger         = Logger.getLogger(SanisKeycloakMapping.class);
    private static final EnumMap<UserInfoAttribute, IValueMapper> personMapping  = initPerson();
    private static final EnumMap<UserInfoAttribute, IValueMapper> kontextMapping = initKontext();

    private static EnumMap<UserInfoAttribute, IValueMapper> initPerson()
    {
        EnumMap<UserInfoAttribute, IValueMapper> personMapping = new EnumMap<>(UserInfoAttribute.class);
        personMapping.put(PERSON_FAMILIENNAME, new BaseMapper("$.person.name.familienname"));
        personMapping.put(PERSON_VORNAME, new BaseMapper("$.person.name.vorname"));
        personMapping.put(PERSON_GEBURTSDATUM, new BaseMapper("$.person.geburt.datum"));
        personMapping.put(PERSON_GEBURTSORT, new BaseMapper("$.person.geburt.geburtsort"));
        personMapping.put(PERSON_VOLLJAEHRIG, new UpperCaseMapper("$.person.geburt.volljaehrig"));
        personMapping.put(PERSON_GESCHLECHT, new UpperCaseMapper("$.person.geschlecht"));
        personMapping.put(PERSON_LOKALISIERUNG, new BaseMapper("$.person.lokalisierung"));
        personMapping.put(PERSON_VERTRAUENSSTUFE, new UpperCaseMapper("$.person.vertrauensstufe"));
        personMapping.put(PERSON_FAMILIENNAME_INITIALEN, new BaseMapper("$.person.name.initialenFamilienname"));
        personMapping.put(PERSON_VORNAME_INITIALEN, new BaseMapper("$.person.name.initialenVorname"));
        return personMapping;
    }

    private static EnumMap<UserInfoAttribute, IValueMapper> initKontext()
    {
        EnumMap<UserInfoAttribute, IValueMapper> kontextMapping = new EnumMap<>(UserInfoAttribute.class);
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ID, new BaseMapper("$.personenkontexte[#].id"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ORG_ID, new BaseMapper("$.personenkontexte[#].organisation.orgid"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ORG_KENNUNG, new BaseMapper("$.personenkontexte[#].organisation.kennung"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ORG_NAME, new BaseMapper("$.personenkontexte[#].organisation.name"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ORG_TYP, new UpperCaseMapper("$.personenkontexte[#].organisation.typ"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ROLLE, new UpperCaseMapper("$.personenkontexte[#].rolle"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_STATUS, new UpperCaseMapper("$.personenkontexte[#].personenstatus"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_LOESCHUNG, new UpperCaseMapper("$.personenkontexte[#].loeschung.zeitpunkt"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_GRUPPEN, new GruppenMapper("$.personenkontexte[#].gruppen"));
        return kontextMapping;
    }

    @Override
    public void addAttributesToResource(Object resource, String userInfo)
    {
        try {
            Object document = Configuration.defaultConfiguration().jsonProvider().parse(userInfo);
            personMapping.forEach((uia, mapper) -> addAttributeToResource(uia, mapper, document, resource, null));
            Integer numberOfKontexte = JsonPath.read(document, "$.personenkontexte.length()");
            if (numberOfKontexte != null) {
                for (int i = 0; i < numberOfKontexte; i++) {
                    addPersonkontextToContext(i, document, resource);
                }
            }
        } catch (JsonPathException e) {
            logger.errorf("Error while reading personInfo json - %s", e.getMessage());
        }

    }

    /**
     * Add user info attribute to resource
     * (@{@link org.keycloak.broker.provider.BrokeredIdentityContext} or
     * {@link org.keycloak.models.UserModel})
     */
    private void addAttributeToResource(UserInfoAttribute uia, IValueMapper mapper, Object document, Object resource, Integer index)
    {
        if (mapper != null) {
            try {
                String attributeName = uia.getAttributeName();
                String jsonPath = mapper.getJsonPath();
                if (index != null) {
                    attributeName = attributeName.replace("#", index.toString());
                    jsonPath = jsonPath.replace("#", index.toString());
                }
                List<String> values = mapper.mapValue(document, jsonPath);
                for (int i = 0; i < values.size(); i++) {
                    String attribute = values.size() == 1 ? attributeName : String.format("%s[%d]", attributeName, i);
                    Iterator<String> iterator = Splitter.fixedLength(255).split(values.get(i)).iterator();
                    String value = iterator.next();
                    setAttribute(resource, attribute, value, null);
                    int overflow = 1;
                    while (iterator.hasNext()) {
                        value = iterator.next();
                        setAttribute(resource, attribute, value, overflow++);
                    }
                }
            } catch (PathNotFoundException e) {
                logger.debugf("Path not found for %s", mapper.getJsonPath());
            }
        }
    }

    private static void setAttribute(Object resource, String attributeName, String value, Integer overflow)
    {
        String key = overflow == null ? attributeName : attributeName + "_" + overflow;
        if (resource instanceof BrokeredIdentityContext) {
            ((BrokeredIdentityContext)resource).setUserAttribute(key, value);
        }
        else if (resource instanceof UserModel) {
            ((UserModel)resource).setSingleAttribute(key, value);
        }
    }

    /**
     * Add person context to resource.
     */
    private void addPersonkontextToContext(int index, Object personenkontext, Object resource)
    {
        kontextMapping.forEach((uia, mapper) -> addAttributeToResource(uia, mapper, personenkontext, resource, index));
    }

}
