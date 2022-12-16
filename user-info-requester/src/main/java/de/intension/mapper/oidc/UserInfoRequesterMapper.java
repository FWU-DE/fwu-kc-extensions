package de.intension.mapper.oidc;

import static de.intension.mapper.RequesterMapperConstants.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.mappers.UserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.utils.StringUtil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.intension.rest.IKeycloakApiMapper;
import de.intension.rest.RestClient;
import de.intension.rest.sanis.SanisKeycloakMapping;

public class UserInfoRequesterMapper extends UserAttributeMapper
{

    public static final String                         PROVIDER_ID                  = "vidis-info-request-mapper_oidc";
    protected static final Logger                      logger                       = Logger.getLogger(UserInfoRequesterMapper.class);
    private static final IKeycloakApiMapper            sanisMapping                 = new SanisKeycloakMapping();

    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = Collections.singleton(IdentityProviderSyncMode.IMPORT);

    private static final List<ProviderConfigProperty>  configProperties             = new ArrayList<>();

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
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode)
    {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
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
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel,
                                            BrokeredIdentityContext context)
    {
        String userInfo = getUserInfo(mapperModel, context);
        if (userInfo != null) {
            sanisMapping.addAttributesToResource(context, userInfo);
        }
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel,
                                   BrokeredIdentityContext context)
    {
        String userInfo = getUserInfo(mapperModel, context);
        if (userInfo != null) {
            sanisMapping.addAttributesToResource(user, userInfo);
        }
    }

    /**
     * Get detailed user information from SANIS REST-API for the current authenticated user.
     */
    private String getUserInfo(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context)
    {
        String endpointUrl = mapperModel.getConfig().get(REST_API_URL_NAME);
        String userInfo = null;
        if (StringUtil.isNotBlank(endpointUrl)) {
            try {
                URL url = new URL(endpointUrl);
                String accessToken = getAccessToken(context);
                if (accessToken != null) {
                    userInfo = RestClient.get(url, accessToken);
                }
                else {
                    logger.errorf("Access Token is null inside BrokeredIdentityContext for IdP %s", context.getIdpConfig().getAlias());
                }
            } catch (MalformedURLException e) {
                logger.errorf("%s - Malformed URL: %s", REST_API_URL_LABEL, endpointUrl);
            } catch (IOException e) {
                logger.errorf(e, "Error while calling rest endpoint %s", endpointUrl);
            }
        }
        else {
            logger.errorf("Field '%s' must not be empty for IdP-Mapper. %s", REST_API_URL_LABEL, MAPPER_CATEGORY);
        }
        return userInfo;
    }

    /**
     * Get ACCESS_TOKEN from identity context.
     */
    private String getAccessToken(BrokeredIdentityContext context)
    {
        String accessToken = null;
        if (context != null && context.getToken() != null) {
            JsonObject jsonObject = JsonParser.parseString(context.getToken()).getAsJsonObject();
            JsonElement at = jsonObject.get(OAuth2Constants.ACCESS_TOKEN);
            if (at != null) {
                accessToken = at.getAsString();
            }
        }
        return accessToken;
    }

}
