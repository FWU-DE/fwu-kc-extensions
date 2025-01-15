# ACR authenticators

For the ideal working of this authenticator the clients should have default values set in the advanced
tab of the client settings. Use the below steps to configure it.

Go the desired client and select the advanced tab from the list of tabs at the top

<img src="../docs/acr-values-authenticator/ClientSettings.png" width="70%"/>

Then scroll down to the ACR to LOA mapping.

<img src="../docs/acr-values-authenticator/AcrToLoaMapping.png" width="70%"/>

Configure the mapping as per the need but make sure to add the value to be a numeric value and key to
be a string which will be used in the default acr values section.

For more details you can refer this link: https://www.keycloak.org/docs/latest/server_admin/#_mapping-acr-to-loa-realm

## ACR values authenticator

This authenticator adds the `acr_values` parameter to the request which is redirected to the IDP. This is done via Client Note.

### Configuration

In the authentication flow of your choice select the option to add authenticator

<img src="../docs/acr-values-authenticator/add_step.png" width="70%"/>

In the list of authenticator search for ACR values authenticator and select it

<img src="../docs/acr-values-authenticator/add_acr_authenticator.png" width="70%"/>

Add it as a required step in the authentication flow.

<img src="../docs/acr-values-authenticator/required_step.png" width="70%"/>

After adding it bind this flow as the browser flow by selecting the Action dropdown on the
top right corner of the screen and then selecting 'Bind Flow'.

<img src="../docs/acr-values-authenticator/browser_flow.png" width="70%"/>

Select browser flow from the dropdown list and save the authentication flow

<img src="../docs/acr-values-authenticator/save_browser_flow.png" width="70%"/>

### Behaviour

If the user tries to login to a specific client and

* the client has the default ACR values set, then the first value is set in the client session which will be added to
the IDP request as param `acr_values`
* the client does not have default acr values but in the OIDC auth request `acr_values` were requested the authenticator
checks for the values from the ACR to LOA mapping table and adds the corresponding values in the IDP request
* the client only has ACR to LOA mapping the first mapping is considered as default and passed to the IDP request
* the client has no configuration of ACR to LOA then a warning is logged

Please note that this will never set the `context.failure()` because this is something which is optional and not requested
by every client.

## ACR denying authenticator

This authenticator denies access to a client in the post login flow when a user does not have an attribute `acr_values` set with an ACR key from the client/realm ACR to LOA configuration.

### Configuration

In the authentication flow of your choice select the option to add authenticator

<img src="../docs/acr-values-authenticator/add_acr_deny.png" width="70%"/>

Make sure it is set as a required step!

### Behaviour

When configured correctly, this extension makes sure requests cannot be fiddled with during the authentication flow.

### Local testing

The test setup is as follows:
- The Realm `idp` acts as an identity provider for Realm `fwu`
- The user federation "IDP Login" in Realm `fwu` uses client `idp-broker` from realm `idp`
- This client is configured with a mapper to set claim `acr_values` with value `mfa`
- The user federation has a mapper to write claim `acr_values` to user attribute with same name

<img src="../docs/acr-values-authenticator/acr-loa-idp-flow.png" width="70%"/>

#### Negative case

To verify the denial of login works, you can either:
- Change the URL's value during login for parameter `acr_values`
- Update the claim value property for the hardcoded claim mapper "ACR" in the `idp` realm client `idp-broker`
