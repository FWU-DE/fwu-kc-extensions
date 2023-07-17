# Change Log

All notable changes to this project will be documented here.


## Version [3.11.0]

- Feature: New KC extension which publishes the login event of the user to Rabbitmq

## Version [3.10.0]

- Update: Support new Authentication in whitelist-authenticator for iam-service 1.1.0

## Version [3.9.0]

- Improvement: Add logging for malformed JSON-responses from whitelist-authenticator
- Testing: Add Config Test to Pre-/Post-Release Postman Collection

## Version [3.8.1]

- Fix: Remove registered user without sessions

## Version [3.8.0]

- Update: Support SANIS API V1.0003.

## Version [3.7.1]

- Improvement: Whitelist authenticators validates user by the federated identities
- Fix: Whitelist authenticators stores Identity Provider into User-Attribute to fallback if no IDP could be determined

## Version [3.7.0]

- Improvement: Acronym Mapper supports now camel case acronyms as well.

## Version [3.6.1]

Fix: Whitelist authenticator to support Post Login Flow

## Version [3.6.0]

- New Feature: Client Mapper to include pseudonyms of the user for other clients 

## Version [3.5.1]

Fix: Whitelist authenticator to support Post Login Flow

## Version [3.5.0]

- Improvement: School whitelist configuration is now read from a REST-API
- Improvement: IdP whitelist configuration is now read from a REST-API

## Version [3.4.0]

- New Feature: School whitelist now supports Flag to allow all schools.


## Version [3.3.0]

- New feature: Prefixed attribute mapper for SAML identity providers

## Version [3.2.1]

- Fix whitelist authenticator to fallback to `kc_idp_hint` if `vidis_idp_hint` not set

## Version [3.2.0]

- New feature: OIDC HMAC Pairwise mapper for email

## Version [3.1.1]

- Update extensions and docker-compose-file to use keycloack 20.0.2

## Version [3.1.0]

- New feature: Support of reading user details from SANIS-API and forwarding to the connected Service Providers
  - OIDC Mapper to request school and user details from SANIS-API
  - Protocol Mapper to forward additional school and user details to the connected Service Providers
  - API model of the Vidis adaptation of SANIS-API (compatible with SANIS)

## Version [3.0.0]

- Update extensions and docker-compose-file to use keycloack 20.0.1

## Version [2.4.0]

- New feature: Prefixed attribute mapper - Prefixes single- and multi-valued claim values

## Version [2.3.0]

- New Feature: Acroynm mapper - Identity provider mapper that will combine the first two letters of the first and last name to a lowercase acronym.

## Version [2.2.1]

- Fixed Nullpointer Exception on infinispan session search

## Version [2.2.0]

- Whitelist Authenticator which checks valid combinations of Client ID and School ID information.
- Fix the NPE on the RemoveExpiredSessionUsers while querying the user session from infinispan

## Version [2.1.0]

- Authenticators return parameter 'kc_idp_hint' if config is partly missing
- Authenticators use parameter 'kc_idp_hint' if configured one is not found

## Version [2.0.0]

- Updated Keycloak versions for extensions to `18.0.2`.

## Version [1.5.1]

- Changed error message for missing IdP configuration

## Version [1.5.0]

- Authenticators return parameter 'kc_idp_hint' if config is partly missing
- Authenticators use parameter 'kc_idp_hint' if configured one is not found

## Version [1.4.1]

- Authenticators return parameter 'kc_idp_hint' if config is completely missing

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
