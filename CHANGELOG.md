# Change Log
All notable changes to this project will be documented here.

## Version [1.4.0]

- Whitelist Authenticator allows IdP hint parameter name to be configured
- Custom identity provider authenticator which allows IdP hint parameter name to be configured
- Error message for missing identity provider in whitelist was improved

## Version [1.3.0]

- Whitelist Authenticator must also work in the context of First Broker Login Flow

## Version [1.2.1]

- Fixed NPE on the remove user on logout SPI

## Version [1.2.0]

- New Keycloak extension that extends the existing **Attribute Importer** with the ability to map incoming values to other values

## Version [1.1.1]

- Allowed `kc_idp_hint` to be missing for the whitelist authenticator

## Version [1.1.0]

- Authenticator extension which rejects authentication if client does not match a whitelist from selected IdP

## Version [1.0.1]

- Removed old HMAC Pairwise mapper
- Refactored new HMAC Pairwise mapper
  - uses hostname of sector identifier URI
  - sector identifier URI return value not validated anymore
  - possibility to encode any user property or attribute

## Version [1.0.0]
Initial release containing following keycloak extensions
- HMAC Pairwise mapper to pseudonymize user IDs
- HMAC Pairwise mapper with static sector identifier
- Remove users from keycloak on logout or on session expiry
