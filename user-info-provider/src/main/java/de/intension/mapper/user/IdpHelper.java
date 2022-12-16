package de.intension.mapper.user;

import java.util.Optional;

import org.keycloak.models.*;
import org.keycloak.utils.StringUtil;

public class IdpHelper
{

    /**
     * Get Idp details from sessions.
     */
    public IdentityProviderModel getIdpAlias(KeycloakSession keycloakSession, UserSessionModel session)
    {
        IdentityProviderModel idpModel = null;
        String idpAlias = null;
        Optional<String> alias = session.getUser().getAttributeStream(UserModel.IDP_ALIAS).findFirst();
        if (alias.isPresent()) {
            idpAlias = alias.get();
        }
        else {
            Optional<FederatedIdentityModel> federatedIdentityModel = keycloakSession.users()
                .getFederatedIdentitiesStream(session.getRealm(), session.getUser()).findFirst();
            if (federatedIdentityModel.isPresent()) {
                idpAlias = federatedIdentityModel.get().getIdentityProvider();
            }
        }
        if (StringUtil.isNotBlank(idpAlias)) {
            idpModel = session.getRealm().getIdentityProviderByAlias(idpAlias);
        }
        return idpModel;
    }

}
