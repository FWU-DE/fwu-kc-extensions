# FWU Keycloak extensions

[![Test and deploy](https://github.com/FWU-DE/fwu-kc-extensions/actions/workflows/main.yaml/badge.svg)](https://github.com/FWU-DE/fwu-kc-extensions/actions/workflows/main.yaml)

This Java x Maven project holds the Keycloak extensions for customizations of the [FWU](https://fwu.de/) Keycloak.
The goal of these extensions are to make the authentication more secure and anonymous.

## Setup for testing

Run the script `start_for_testing.sh` in the root directory to start the Keycloak.

This will start a Keycloak docker container and others specified in the [`docker-compose.yaml`](./test/docker-compose.yaml).


## Features

### HMAC Pairwise subject with static sector identifier

This OIDC mapper can be used to pseudonymize one of the attributes of user like `id`, `username` for different clients and has the following features. For more details please check [here](./hmac-mapper/README.md)  

1. Field `sectorIdentifier` is mandatory
2. Field `sectorIdentifier` has to be still a valid URI but must not link to a JSON-File
3. If `sectorIdentifier` links to a JSON-File, it's content will be completely ignored

### Remove user on logout

It holds the customization to remove the user on logout or session expiration. For more details please check [here](./remove-user-on-logout/README.md).

## POC - Whitelist Authenticator

This Authenticator checks valid combinations of Client ID and KC_IDP_HINT information.
For more details please check [here](./whitelist-authenticator/README.md).


## Contributing

To learn more about how you can contribute to this project, check out [`CONTRIBUTING.md`](CONTRIBUTING.md).
