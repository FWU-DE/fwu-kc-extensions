# FWU Keycloak extensions

[![Test and deploy](https://github.com/FWU-DE/fwu-kc-extensions/actions/workflows/main.yaml/badge.svg)](https://github.com/FWU-DE/fwu-kc-extensions/actions/workflows/main.yaml)

This Java x Maven project holds the Keycloak extensions for customizations of the [FWU](https://fwu.de/) Keycloak.
The goal of these extensions are to make the authentication more secure and anonymous.

## Setup for testing

Run the script `start_for_testing.sh` in the root directory to start the Keycloak.
This will start a Keycloak docker container and others specified in the [`docker-compose.yaml`](test/docker-compose.yaml).

## Features

### HMAC Pairwise subject identifier

This OIDC mapper can be used to pseudonymize the user ID for different clients. For more details please check [here](./hmac-mapper/README.md).

### HMAC Pairwise subject with static sector identifier

This is an extended version of the **HMAC Pairwise subject identifier** with the following additional features:
1. Field `sectorIdentifier` is mandatory
2. Field `sectorIdentifier` has to be still a valid URI but must not link to a JSON-File
3. If `sectorIdentifier` links to a JSON-File, it's content will be completely ignored

For more details please check [here](./hmac-mapper/README.md).
All changes related to the new version are tagged with `#NEW_V2`.

### Remove user on logout

It holds the customization to remove the user on logout or session expiration. For more details please check [here](./remove-user-on-logout/README.md).
