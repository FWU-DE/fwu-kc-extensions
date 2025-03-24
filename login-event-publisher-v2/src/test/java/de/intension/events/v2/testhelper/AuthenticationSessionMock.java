package de.intension.events.v2.testhelper;

import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mockito;

public abstract class AuthenticationSessionMock
    implements AuthenticationSessionModel
{

    private UserModel authenticatedUser;

    public static AuthenticationSessionMock create(UserModel user)
    {
        AuthenticationSessionMock inst = Mockito.mock(AuthenticationSessionMock.class, Mockito.CALLS_REAL_METHODS);
        inst.authenticatedUser = user;
        return inst;
    }

    @Override
    public UserModel getAuthenticatedUser()
    {
        return authenticatedUser;
    }
}
