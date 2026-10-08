package de.intension.mapper.user;

import org.keycloak.models.*;
import org.keycloak.utils.StringUtil;

import java.util.Optional;

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
            idpModel = keycloakSession.identityProviders().getByAlias(idpAlias);
        }
        return idpModel;
    }
}
