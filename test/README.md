# Setup for testing

Run the script `start_for_testing.sh` in the root directory to start the Keycloak.
This will start a Keycloak docker container and others specified in the [`docker-compose.yaml`](docker-compose.yaml).

## Pairwise subject identifier

The clients `slowpoke` and `drowzee` in the realm `fwu` use a *Pairwise subject identifier* mapper to pseudonymize User IDs when a token is issued. For validating a request, the Sector identifier URI 'http://172.18.0.2:8000/sector_identifiers.json' provided by the docker container [`sector_identifiers`](docker-compose.yaml#L20) is used.

The client `grimer` also uses a *Pairwise subject identifier* mapper, but doesn't specify a Sector identifier URI. Thus the root URL of the client is used to hash the user ID.

<img src="../docs/pseudo/ppid_sector_id.svg" width="70%"/>

### Configuration

Prerequesite is a unique host for the redirect URIs. A client which has multiple host names configured via valid redirect URIs cannot be used for the *Pairwise subject identifier* mapper!

#### Invalid example

![Without a configured Sector Identifier URI, client redirect URIs must not contain multiple host components](../docs/pseudo/multiple_redirect_hosts.png)

#### Valid example

![Configure the mapper](../docs/pseudo/redirect_uri_config.png)

Create a mapper for the desired client and enter your desired values.

<img src="../docs/pseudo/mapper_config.png" width="70%"/>

1. The **Sector Identifier URI** can be used to group clients and must return a JSON array with all the hosts for the clients which use this URI.
   ![Example array of sector identifiers](../docs/pseudo/sector_identifiers.png)
2. The **Salt** is the second factor used when hashing the user ID with `SHA-256`. When no salt is configured on creation a random one will be generated.

### How it's working

The *Pairwise subject identifier* mapper always hashes the user's ID with a host and the configured salt. This value is then mapped to the `sub` attribute in the returned token. 
Meaning, when two clients have the same host and salt, they will return the same `sub` for a given user ID. 

When no Sector identifier URI is configured for the mapper, the host of the valid redirect URIs configured in the client will be used.

### Testing

The Keycloak is configured with two test users and four clients with *Pairwise subject identifier* mappers configured. The exact configuration of the mappers can be seen in the followin diagram.

<img src="../docs/pseudo/test_setup.png" width="70%"/>

You can use the [Postman](https://postman.io) collection '[`FWU.postman_collection.json`](FWU.postman_collection.json)' with the environment '[`FWU.postman_environment.json`](FWU.postman_environment.json)' for testing purposes:
1. `GET` the sector identifiers to see what the mapper will use
2. `GET` the user ID of a user for later
3. `POST` a login request to the `slowpoke` client (with sector identifier URI and salt configured)
4. `POST` a login request to the `drowzee` client (with same sector identifier URI and salt configured)
5. `POST` a login request to the `grimer` client (same salt - no sector identifier URI)
6. `POST` a login request to the `muk` client (different salt than `grimer`, but same host configured)
7. `POST` a login request to the `slowpoke` client as a different user

Outcome:
1. The `sub` in the returned access tokens is not the user's ID
2. The `sub` in the access tokens returned by `slowpoke` and `drowzee` are the same because they share the same sector identifier and salt
3. The `sub` in the access tokens returned by `slowpoke` and `grimer` are **not** the same because the latter is not part of the sector
4. The `sub` in the access tokens returned by `grimer` and `muk` are **not** the same because they don't share the same salt
5. The `sub` in the access tokens returned by `slowpoke` are different for each user