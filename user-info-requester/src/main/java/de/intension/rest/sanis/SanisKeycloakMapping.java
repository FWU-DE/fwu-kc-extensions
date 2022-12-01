package de.intension.rest.sanis;

import static de.intension.api.UserInfoAttribute.*;

import java.util.EnumMap;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.UserModel;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import com.jayway.jsonpath.PathNotFoundException;

import de.intension.api.UserInfoAttribute;
import de.intension.rest.BaseMapper;
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
        personMapping.put(PERSON_GESCHLECHT, new UpperCaseMapper("$.person.geschlecht"));
        personMapping.put(PERSON_LOKALISIERUNG, new BaseMapper("$.person.lokalisierung"));
        personMapping.put(PERSON_VERTRAUENSSTUFE, new BaseMapper("$.person.vertrauensstufe"));
        return personMapping;
    }

    private static EnumMap<UserInfoAttribute, IValueMapper> initKontext()
    {
        EnumMap<UserInfoAttribute, IValueMapper> kontextMapping = new EnumMap<>(UserInfoAttribute.class);
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ID, new BaseMapper("$.personenkontexte[#].ktid"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ORG_ID, new BaseMapper("$.personenkontexte[#].organisation.orgid"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ORG_KENNUNG, new BaseMapper("$.personenkontexte[#].organisation.kennung"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ORG_NAME, new BaseMapper("$.personenkontexte[#].organisation.name"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ORG_TYP, new BaseMapper("$.personenkontexte[#].organisation.typ"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_ROLLE, new BaseMapper("$.personenkontexte[#].rolle"));
        kontextMapping.put(PERSON_KONTEXT_ARRAY_STATUS, new BaseMapper("$.personenkontexte[#].personenstatus"));
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
                String value = JsonPath.read(document, jsonPath);
                if (value != null) {
                    value = mapper.mapValue(value);
                    if (resource instanceof BrokeredIdentityContext) {
                        ((BrokeredIdentityContext)resource).setUserAttribute(attributeName, value);
                    }
                    else if (resource instanceof UserModel) {
                        ((UserModel)resource).setSingleAttribute(attributeName, value);
                    }
                }
            } catch (PathNotFoundException e) {
                logger.debugf("Path not found for %s", mapper.getJsonPath());
            }
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
