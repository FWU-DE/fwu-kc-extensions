# Change Log

All notable changes to this project will be documented here.

## Version [4.19.3]

- Enhancement: Modify the behavior of acr values authenticator to be able to configure and pass any parameter to IDP
- Enhancement: Add the hmaid as user id if hmac mapper is not present for the client

## Version [4.19.2]

- Enhancement: Not denying access to user even in case of no license for the user
- Enhancement: Returning empty object for the user if license is not found

## Version [4.19.0]

- Feature: Fetch licenses for user from different endpoint based on the configuration in client

## Version [4.18.1]

- Bugfix: Allow multiple instances of RabbitMQ connection manager

## Version [4.18.0]

- Feature: New BMI login event publisher
- Improvement: More configuration possibilities for all login event publishers

## Version [4.17.1]

- Bugfix: Add authentication check to LicenceResource

## Version [4.17.0]

- Feature: `Licence` table now contains a `updated_at` column and licences are replaced if they already exist and 
  fetched again from Licence Connect

## Version [4.16.4]

- Bugfix: Check for "licenses" in response if "licences" not set

## Version [4.16.3]

- Bugfix: Added Content-Type header to Licence Connect requests
- Bugfix: Adapted expected Licence Connect reponse format

## Version [4.16.2]

- Technical: Updated variables `GITLAB_TOKEN` and `GITLAB_TRIGGER_TOKEN`

## Version [4.16.1]

- Technical: New image build project in GitLab

## Version [4.16.0]

- Feature: Custom Rest endpoint to fetch licences by HMAC ID
- Technical: Refactored tests using testcontainers again

## Version [4.15.0]

- Feature: Fetched licences are now persisted together with the hmacId
- Feature: The persisted licences are also deleted once the user logs out
- The modules `remove-user-on-logout` and `licence-connect-authenticator` are combined into a single 
  module `user-licence-lifecycle-manager`

## Version [4.14.2]

- Bugfix: new logic for ACR deny authenticator

## Version [4.14.1]

- Bugfix: get version to build from correct step

## Version [4.14.0]

- Improvement: Rename "license" to "licence" where possible for unification

## Version [4.13.0]

- Feature: Add new authenticator for sending acr_values to the IDP request

## Version [4.12.0]

- Feature: Add new authenticator to fetch the license for user from license connect

## Version [4.11.5]

- Bugfix: Add JsonPath dependency to 3rd Party Libs

## Version [4.11.4]

- Bugfix: Don't delete users without created Date
- Improvement: Add possiblity to configure the Toleration period for newly Created Users to not be deleted.

## Version [4.11.3]

- Bugfix: Simple HMAC mapper can map to configurable claim

## Version [4.11.2]

- Bugfix: Publish to GitLab packages with Maven

## Version [4.11.1]

- Previous release artifacts were broken by GitHub

## Version [4.11.0]

- Feature: Add new protocol mapper to allow HMAC hashing of user IDs based on very simple sector identifier

## Version [4.10.2]

- bugfix: Fix userquery since created_timestamp is seconds not millis

## Version [4.10.1]

- bugfix: Fix user deletion when not only idp-users but all users should be deleted

## Version [4.10.0]

- Improvement: Add possibility to also delete users not provided by an idenetity provider

## Version [4.9.1]

- Patch: Improve CSS for account linking form

## Version [4.9.0]

- Feature: Add new protocol mapper based on role to get the user info in the tokens
- Feature: Update the existing vidis-info-mapper to consider roles in the user attributes before adding it to claims

## Version [4.8.0]

- Improvement: update error messages for both whitelist authenticators

## Version [4.7.1]

- update: reduce wait time before user deletion
- bugfix: count deleted users only

## Version [4.7.0]

- Feature: Custom REST-Endpoint for deleting inactive users
- Improvement: No internal user removal tasks created for user registration or login

## Version [4.6.0]

- Improvement: HMAC mapping admin resource
	- Added additional logging

## Version [4.5.0]

- Improvement: HMAC mapping admin resource
	- Authenticate on different realm

## Version [4.4.0]

- Improvement: HMAC mapping resource
  - Cross realm allowed
  - Changed resource to custom admin rest api

## Version [4.3.0]

- New Feature: Account linking of two users from different identity providers

## Version [4.2.0]

- Feature: Endpoint to check HMAC encrypted value in list of unencrypted values

## Version [4.1.0]

- Improvement: Login-Event-Listener now adds schoolids of the user to the events

## Version [4.0.0]

- Update: Update to Keycloak 22

## Version [3.11.3]

- Improvement - Prefix mapper now supports regular expressions

## Version [3.11.2]

- Revert Changes from 3.10.0- Only use whitelist-authenticator via client-credentials

## Version [3.11.1]

- Improvement: Add loggers for rabbitmq connection

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
