package de.intension.mapper.saml;

import static de.intension.mapper.RequesterMapperConstants.*;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.mappers.UserAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

public class UserInfoRequesterMapper extends UserAttributeMapper
{

    public static final String                        PROVIDER_ID      = "user-info-request-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property = new ProviderConfigProperty();
        property.setName(REST_API_URL_NAME);
        property.setLabel(REST_API_URL_LABEL);
        property.setHelpText(REST_API_URL_HELPTEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
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
    public String getDisplayCategory()
    {
        return MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType()
    {
        return MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText()
    {
        return MAPPER_HELPTEXT;
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                                   BrokeredIdentityContext context)
    {
        //TODO implement
        /*
        user.setSingleAttribute(UserInfoAttribute.HEIMATORGANISATION_NAME.getAttributeName(), "DE-SN-Schullogin");
        user.setSingleAttribute(UserInfoAttribute.HEIMATORGANISATION_BUNDESLAND.getAttributeName(), "DE-BY");
        user.setSingleAttribute(UserInfoAttribute.PERSON_FAMILIENNAME.getAttributeName(), "Muster");
        user.setSingleAttribute(UserInfoAttribute.PERSON_VORNAME.getAttributeName(), "Max");
        user.setSingleAttribute(UserInfoAttribute.PERSON_GEBURTSDATUM.getAttributeName(), "2010-01-01");
        user.setSingleAttribute(UserInfoAttribute.PERSON_GESCHLECHT.getAttributeName(), "d");
        user.setSingleAttribute(UserInfoAttribute.PERSON_LOKALISIERUNG.getAttributeName(), "de-DE");
        user.setSingleAttribute(UserInfoAttribute.PERSON_VERTRAUENSSTUFE.getAttributeName(), "VOLL");
        user.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ORG_KENNUNG.getAttributeName(), "NI_12345");
        user.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ORG_NAME.getAttributeName(), "Muster-Schule");
        user.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ORG_TYP.getAttributeName(), "SCHULE");
        user.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_ROLLE.getAttributeName(), "LERN");
        user.setSingleAttribute(UserInfoAttribute.PERSON_KONTEXT_STATUS.getAttributeName(), "AKTIV");
        */
    }

}
