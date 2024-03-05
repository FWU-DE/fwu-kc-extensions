# Remove user on logout

This extension is to make sure the user is removed from keycloak on the user logout and session expiration.

## Configurations

### Custom listener

Remove user on logout event listener should be configured to enable the removal of the user on logout.

<img src="listener-config.png" width="500" />

### Custom authentication flow

Custom authentication flow should be configured to disable the profile review like below,

1. Open **Realm settings**  > **Authentication** > **Flows**.
2. Select the `First  Boker Lgin` and copy
3. In the copy, disable `Review Profile(review profile config)`

<img src="authentication-flow.png" width="500" /> 

### IDP configuration

Any idp configured should use the copied flow as first login flow like below,

<img src="idp-config.png" width="500" /> 


### Custom rest api endpoint

A special REST-API endpoint has been provided to cyclically delete users whose session has timed out.
When this endpoint is called, all users who do not have an active session (OFFLINE session is ignored) and have a link to an IdP are deleted.
The maximum number of data records to be deleted can be specified as a query parameter (limited to 1000 data records)

https://<keycloak-host>/auth/admin/realms/<realm-name>/vidis-custom/users/inactive?max={numberOfUserToDelete}
e.g. https://keycloak-test.ded/auth/admin/realms/test/users/inactive?max=500