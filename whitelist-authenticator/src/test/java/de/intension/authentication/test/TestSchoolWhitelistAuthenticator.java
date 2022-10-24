package de.intension.authentication.test;

import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;

import de.intension.authentication.schools.SchoolWhitelistAuthenticator;

/**
 * This class is used to simplify JUnit tests.
 * !!! IMPORTANT: This class is for JUnit tests only!!!
 */
public class TestSchoolWhitelistAuthenticator
        extends SchoolWhitelistAuthenticator
{

    /**
     * Do nothing and return null, because ErrorPage must not be created in case of unit tests.
     */
    @Override
    protected Response createErrorPage(AuthenticationFlowContext context)
    {
        return null;
    }
}
