package de.intension.acronym;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;

/**
 * LDAP mapper that will combine the first two letters of the first and last name to a lowercase acronym.
 */
public class AcronymMapper extends AbstractLDAPStorageMapper {

    public static final String USER_MODEL_ATTRIBUTE = "user.model.attribute";

    public AcronymMapper(ComponentModel mapperModel, LDAPStorageProvider ldapProvider) {
        super(mapperModel, ldapProvider);
    }

    @Override
    public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean b) {
        var firstName = user.getAttributeStream(UserModel.FIRST_NAME).findFirst().orElse("");
        var lastName = user.getAttributeStream(UserModel.LAST_NAME).findFirst().orElse("");

        user.setSingleAttribute(mapperModel.getConfig().getFirst(USER_MODEL_ATTRIBUTE),
                AcronymUtil.createAcronym(firstName, lastName));
    }

    @Override
    public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm) {

    }

    @Override
    public UserModel proxy(LDAPObject ldapUser, UserModel user, RealmModel realm) {
        return user;
    }

    @Override
    public void beforeLDAPQuery(LDAPQuery ldapQuery) {

    }
}
