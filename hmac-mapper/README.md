# HMAC Pairwise subject identifier (v1 & v2)

All changes related to version two (v2) are tagged with `#NEW_V2`

## Configuration (v1)

Prerequesite is a unique host for the redirect URIs. A client which has multiple host names configured via valid redirect URIs cannot be used for the *HMAC Pairwise subject identifier* mapper!

### Invalid example

![Without a configured Sector Identifier URI, client redirect URIs must not contain multiple host components](../docs/pseudo/multiple_redirect_hosts.png)

### Valid example

![Configure the mapper](../docs/pseudo/redirect_uri_config.png)

Create a mapper for the desired client and enter your desired values.

<img src="../docs/pseudo/mapper_config.png" width="70%"/>

1. The **Sector Identifier URI** can be used to group clients and must return a JSON array with all the hosts for the clients which use this URI.
   ![Example array of sector identifiers](../docs/pseudo/sector_identifiers.png)
2. The **Salt** is the second factor used when hashing the user ID with `SHA-256`. When no salt is configured on creation a random one will be generated.
3. Finally, the selected algorithm defines the length of the generated hash.

## Configuration (v2) `#NEW_V2`

Create a mapper for the desired client and enter your desired values.

<img src="../docs/pseudo/mapper_config_v2.png" width="70%"/>

1. New mapper *HMAC Pairwise subject with static sectorIdentifier* must be selected.
2. The Sector Identifier URI must be set to group clients.
3. The Salt is the second factor used when hashing the user ID with SHA-256. When no salt is configured on creation a random one will be generated.
4. Finally, the selected algorithm defines the length of the generated hash.

**Hint:** Only one version of HMAC Pairwise mapper can be configured for a client.
Please remove the existing one before or after configuring the new one.

## How it's working

The *HMAC Pairwise subject identifier* mapper always hashes the user's ID with a host and the configured salt. This value is then mapped to the `sub` attribute in the returned token.
Meaning, when two clients have the same host and salt, they will return the same `sub` for a given user ID. 

When no Sector identifier URI is configured for the mapper, the host of the valid redirect URIs configured in the client will be used.

`#NEW_V2` - Sector Identifier URI is mandatory and therefore redirect URIs are never be used by mapper *HMAC Pairwise subject with static sectorIdentifier*.

<img src="../docs/pseudo/ppid_sector_id.svg" width="70%"/>

## Testing

The Keycloak is configured with two test users and four clients with *HMAC Pairwise subject identifier* mappers configured. The exact configuration of the mappers can be seen in the following diagram.

`#NEW_V2` -
In addition, two new clients have been configured for *HMAC Pairwise subject with static sectorIdentifier* mapper.
The clients `applicy/applicy_ext` and `progrant/progrant_ext` are equal configured except `Mapper Type` and `Sector Identifier URI`.

<img src="../docs/pseudo/test_setup.png" width="70%"/>

The clients `applicy` and `progrant` in the realm `fwu` use a *HMAC Pairwise subject identifier* mapper to pseudonymize User IDs when a token is issued. The algorithm used to pseudonymize the ID is part of the [HMAC](https://datatracker.ietf.org/doc/html/rfc2104) family - the exact algorithm can be configured for on the mapper. For validating a request, the Sector identifier URI 'http://172.18.0.2:8000/sector_identifiers.json' provided by the docker container [`sector_identifiers`](docker-compose.yaml#L20) is used.

`#NEW_V2` -
The clients `applicy_ext` and `progrant_ext` in the realm `fwu` use a *HMAC Pairwise subject with static sectorIdentifier* mapper to pseudonymize User IDs when a token is issued. The algorithm used to pseudonymize the ID is part of the [HMAC](https://datatracker.ietf.org/doc/html/rfc2104) family - the exact algorithm can be configured for on the mapper. For validating a request, the Sector identifier URI 'http://a-static-url.de/sector_identifiers.json' is used without existence or content check.

The client `formand` also uses a *HMAC Pairwise subject identifier* mapper, but doesn't specify a Sector identifier URI. Thus the root URL of the client is used to hash the user ID.

You can use the [Postman](https://postman.io) collection '[`FWU.postman_collection.json`](FWU.postman_collection.json)' with the environment '[`FWU.postman_environment.json`](FWU.postman_environment.json)' for testing purposes:
1. `GET` the sector identifiers to see what the mapper will use
2. `GET` the user ID of a user for later
3. `POST` a login request to the `applicy` client (with sector identifier URI and salt configured)
4. `POST` a login request to the `applicy_ext` client (with sector identifier URI and salt configured) - `#NEW_V2`
5. `POST` a login request to the `progrant` client (with same sector identifier URI and salt configured)
6. `POST` a login request to the `progrant_ext` client (with same sector identifier URI and salt configured) - `#NEW_V2`
7. `POST` a login request to the `formand` client (same salt - no sector identifier URI)
8. `POST` a login request to the `desight` client (different salt than `formand`, but same host configured)
9. `POST` a login request to the `applicy` client as a different user

Outcome:
1. The `sub` in the returned access tokens is not the user's ID
2. The `sub` in the access tokens returned by `applicy` and `progrant` are the same because they share the same sector identifier and salt
3. The `sub` in the access tokens returned by `applicy_ext` and `progrant_ext` are the same because they share the same sector identifier and salt  - `#NEW_V2`
4. The `sub` in the access tokens returned by `applicy` and `formand` are **not** the same because the latter is not part of the sector
5. The `sub` in the access tokens returned by `applicy_ext` and `formand` are **not** the same because the latter is not part of the sector  - `#NEW_V2`
6. The `sub` in the access tokens returned by `formand` and `desight` are **not** the same because they don't share the same salt
7. The `sub` in the access tokens returned by `applicy` are different for each user
