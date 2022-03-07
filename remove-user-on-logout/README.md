# remove-user-on-logout

This extension is to make sure the user is removed from keycloak on the user logout and session expiration.

## Configurations

### Custom listener

Remove user on logout event listener should be configured to enable the removal of the user on logout or session expiration.

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