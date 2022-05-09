# FWU Keycloak extensions

To hold the keycloak extensions for the FWU customizations.

## Setup for testing

Run the script `start_for_testing.sh` in the root directory to start the Keycloak.
This will start a Keycloak docker container and others specified in the [`docker-compose.yaml`](docker-compose.yaml).

## HMAC Pairwise subject identifier

This OIDC mapper can be used to pseudonymize the user ID for different clients. For more details please check [here](./hmac-mapper/README.md).

## Remove user on logout

It holds the customization to remove the user on logout or session expiration. For more details please check [here](./remove-user-on-logout/README.md).