# HMAC Pairwise subject with static sectorIdentifier
## Configuration

Create a mapper for the desired client and enter your desired values.

<img src="../docs/pseudo/mapper_config.png" width="70%"/>

1. New mapper *HMAC Pairwise subject with static sectorIdentifier* must be selected.
2. The Sector Identifier URI must be set to group clients.
3. The Salt is the second factor used when hashing the user ID with SHA-256. When no salt is configured on creation a random one will be generated.
4. Finally, the selected algorithm defines the length of the generated hash.
5. Local sub identifier which gives the unique identifier whose value is to be used during the pseudonymized sub generation. It should be one of the attributes of user like `id`, `username`, etc.

## How it's working

The *HMAC Pairwise subject with static sectorIdentifier* mapper always hashes the selected attribute with a host and the configured salt based on the algorithm selected. This value is then mapped to the `sub` attribute in the returned token.

<img src="../docs/pseudo/ppid_sector_id.svg" width="70%"/>

## Testing

The Keycloak is configured with two test users and four clients with *HMAC Pairwise subject with static sectorIdentifier* mappers configured. The exact configuration of the mappers can be seen in the following diagram.

<img src="../docs/pseudo/test_setup.png" width="70%"/>

The clients `applicy` and `progrant` in the realm `fwu` use a *HMAC Pairwise subject with static sectorIdentifier* mapper to pseudonymize User IDs when a token is issued. The algorithm used to pseudonymize the ID is part of the [HMAC](https://datatracker.ietf.org/doc/html/rfc2104) family - the exact algorithm can be configured for on the mapper. For validating a request, the Sector identifier URI 'http://172.18.0.2:8000/sector_identifiers.json' provided by the docker container [`sector_identifiers`](docker-compose.yaml#L20) is used.

You can use the [Postman](https://postman.io) collection '[`FWU.postman_collection.json`](FWU.postman_collection.json)' with the environment '[`FWU.postman_environment.json`](FWU.postman_environment.json)' for testing purposes:
1. `GET` the sector identifiers to see what the mapper will use
2. `GET` the user ID of a user for later
3. `POST` a login request to the `applicy` client (with sector identifier URI and salt configured)
5. `POST` a login request to the `progrant` client (with same sector identifier URI and salt configured)
6. `POST` a login request to the `formand` client (with same sector identifier URI and salt configured, but different algorithm)
7. `POST` a login request to the `desight` client (with different sector identifier URI than `formand`)
8. `POST` a login request to the `applicy` client as a different user

Outcome:
1. The `sub` in the returned access tokens is not the user's ID
2. The `sub` in the access tokens returned by `applicy` and `progrant` are the same because they share the same sector identifier and salt
3. The `sub` in the access tokens returned by `applicy` and `formand` are **not** the same because the algorithm is not same for the latter
4. The `sub` in the access tokens returned by `formand` and `desight` are **not** the same because they don't share the same sector identifier
5. The `sub` in the access tokens returned by `applicy` are different for each user