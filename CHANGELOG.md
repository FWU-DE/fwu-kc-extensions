# Change Log
All notable changes to this project will be documented here.

## Version [1.1.0]

- Authenticator extension which rejects authentication if client does not match a whitelist from selected IdP

## Version [1.0.1]

- Fixed the issue in generating the sub value based on the configured local sub value

## Version [1.0.0]
Initial release containing following keycloak extensions
- HMAC Pairwise mapper to pseudonymize user IDs
- HMAC Pairwise mapper with static sector identifier
- Remove users from keycloak on logout or on session expiry
