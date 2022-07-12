# Change Log
All notable changes to this project will be documented here.

## Version [2.0.0]

Updated Keycloak versions for extensions to `18.0.2`.

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
