# Account Linking Authenticators

Contains a list of authenticators that are required to link two accounts from different identity providers.

## Authenticator

### Condition - user attribute key

Checks whether a corresponding attribute with the stored name exists for the user.

![auth_user_att_key_select.png](..%2Fdocs%2Faccount-linking%2Fauth_user_att_key_select.png)

#### Configuration

![auth_user_att_key_config.png](..%2Fdocs%2Faccount-linking%2Fauth_user_att_key_config.png)

1. assignment of the name of the configuration
2. name of the attribute to be searched for at the user
3. setting the result logic (NOT)
4. saving the configuration

### Condition - user refers to IdP

Checks whether the user is assigned to a defined identity provider in the context.

![auth_idp_link_select.png](..%2Fdocs%2Faccount-linking%2Fauth_idp_link_select.png)

#### Configuration

![auth_idp_link_config.png](..%2Fdocs%2Faccount-linking%2Fauth_idp_link_config.png)

1. assignment of the name of the configuration
2. name of the Identity Provider alias to be searched for at the user
3. setting the result logic (NOT)
4. saving the configuration

### Account linking target input Form

Form for entering the user name or e-mail address of the second identity provider. The value is stored as an attribute for the user.

![auth_input_form_select.png](..%2Fdocs%2Faccount-linking%2Fauth_input_form_select.png)

#### Configuration

![auth_input_form_config.png](..%2Fdocs%2Faccount-linking%2Fauth_input_form_config.png)

1. assignment of the name of the configuration
2. account linking attribute name, which will be used to store the input value at the user
3. IdentityProvider alias for displaying the target IdP within the description (display name from Keycloak is displayed if available)
4. saving the configuration

## Authentication flow

Short example for the configuration of the idp chaining authentication flow. Must be configured as a post broker flow on the first IdP

![idp_chaining_post_broker_flow.png](..%2Fdocs%2Faccount-linking%2Fidp_chaining_post_broker_flow.png)

To enable the mapping between the user attribute and the second IdP, the following keycloak extension must be used in the first broker flow of the second IdP.

https://github.com/sd-f/keycloak-custom-attribute-idp-linking

## Testing

1. run shell script [./start_for_testing.sh](./../start_for_testing.sh) to start docker containers
2. login into keycloak [admin console](http://localhost:18080/auth/admin)
3. Select realm `fwu`
4. Choose IdP with name `IDP Login` and configure the authentication flow `idp chaining post broker flow` as Post Login Flow
5. Bind the authentication flow `Browser` as the the default Browser flow
6. Use the [account-console](http://localhost:18080/auth/realms/fwu/account/) as the client to test the login flow
7. Choose `IDP Login` as the first Identity Provider and login with user idpuser/test
8. In the next step, you will be asked for the username or e-mail address for the link to the second identity provider account. Please enter "idpuser2@test.de" as the account linking identifier and click on Sign In
9. You will be forwarded to the second Identity Provider `IDP Login 2`
9. Enter password `test` to login (username should be pre-filled)
10. Now, you are logged in and both accounts are linked to the same Keycloak user

![user_view_linked_accounts.png](..%2Fdocs%2Faccount-linking%2Fuser_view_linked_accounts.png)
