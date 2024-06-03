package de.intension.listener;

import de.intension.resources.admin.DeletableUserType;
import org.keycloak.models.UserModel;
import org.keycloak.utils.StringUtil;

public class UserDeletionChecker {

    private UserDeletionChecker() {
    }

    static boolean userShouldBeDeleted(UserModel user, DeletableUserType deletableUserType) {
        return user != null && (deletableUserType == DeletableUserType.ALL || isIdpUser(user));
    }

    private static boolean isIdpUser(UserModel user) {
        return user.getFederationLink() != null
                || user.getAttributes().get("idpAlias").stream().anyMatch(StringUtil::isNotBlank);
    }
}