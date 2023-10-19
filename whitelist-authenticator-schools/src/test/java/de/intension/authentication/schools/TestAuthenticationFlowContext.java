package de.intension.authentication.schools;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;

import jakarta.ws.rs.core.Response;

public abstract class TestAuthenticationFlowContext
    implements AuthenticationFlowContext
{

    private Boolean success = null;

    @Override
    public void success()
    {
        success = Boolean.TRUE;
    }

    @Override
    public void failure(AuthenticationFlowError error, Response response)
    {
        success = Boolean.FALSE;
    }

    @Override
    public void forceChallenge(Response challenge)
    {
        success = challenge.getStatus() == Response.Status.SEE_OTHER.getStatusCode();
    }

    @Override
    public void attempted()
    {
        success = Boolean.FALSE;
    }

    public Boolean getSuccess()
    {
        return success;
    }
}
