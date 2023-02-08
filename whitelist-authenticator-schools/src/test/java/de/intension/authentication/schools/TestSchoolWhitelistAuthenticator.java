package de.intension.authentication.schools;

import javax.ws.rs.core.Response;

import org.keycloak.authentication.AuthenticationFlowContext;

import de.intension.authentication.rest.SchoolAssignmentsClient;

/**
 * This class is used to simplify JUnit tests.
 * !!! IMPORTANT: This class is for JUnit tests only!!!
 */
public class TestSchoolWhitelistAuthenticator
        extends SchoolWhitelistAuthenticator
{

    public TestSchoolWhitelistAuthenticator(String restApiUri)
    {
        super(new SchoolAssignmentsClient("http://localhost:18733/auth", restApiUri));
    }

    public TestSchoolWhitelistAuthenticator()
    {
        super(new SchoolAssignmentsClient("http://localhost:18733/auth", "http://localhost:18733/school-assignments"));
    }

    /**
     * Do nothing and return null, because ErrorPage must not be created in case of unit tests.
     */
    @Override
    protected Response createErrorPage(AuthenticationFlowContext context)
    {
        return null;
    }
}
