package de.intension.events.v2.testhelper;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.mockito.Mockito;

public abstract class UserProviderMock
    implements UserProvider
{

    private Map<String, UserModel> usersById;

    public static UserProviderMock create(UserModel user)
    {
        UserProviderMock inst = Mockito.mock(UserProviderMock.class, Mockito.CALLS_REAL_METHODS);
        inst.usersById = new HashMap<>();
        inst.usersById.put(user.getId(), user);

        return init(inst);
    }

    private static UserProviderMock init(UserProviderMock inst)
    {
        return inst;
    }

    @Override
    public UserModel getUserById(RealmModel realm, String userId)
    {
        return usersById.get(userId);
    }

}
