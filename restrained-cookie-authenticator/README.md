# Restrained cookie authenticator

Keycloak authenticator SPI provider (`restrained-cookie-authenticator`, provider ID
`restrained-cookie-authenticator`) that restrains the SSO-cookie shortcut for logins brokered
through selected identity providers.

This Keycloak instance mainly acts as a broker in front of a number of IdPs. Some of these IdPs
require the full authentication round trip on every login instead of being short-circuited by an
already existing SSO session (the standard Keycloak `Cookie` authenticator step). This
authenticator extends the standard cookie authenticator and additionally forces the full round
trip whenever:

1. the current user is linked to one of the configured `restrainingIdPs`, or
2. the current user has a `pairwiseSub` attribute set (regardless of which IdP is linked) — some
   IdPs provide a `pairwiseSub` value, and users authenticated through them should always go
   through the full round trip.

If neither condition applies, the SSO cookie is honoured as usual and the login is shortcut.

## Configuration

Add the authenticator to an authentication flow via the Keycloak Admin Console
(`Authentication` -> select flow -> `Add execution` -> `Restrained Cookie Authenticator`), in the
same place where the standard `Cookie` authenticator would normally sit (typically as the first,
`ALTERNATIVE` step of the `Browser` flow, followed by the identity provider redirector so that an
`attempted()` outcome falls through to the full broker round trip).

Afterwards open `Actions` -> `Config` on the execution and set:

| Config property   | Description                                                                                   |
|-------------------|-------------------------------------------------------------------------------------------------|
| `restrainingIdPs` | List of identity provider aliases for which cookie-based re-authentication is always restrained |

## How it's working

`RestrainedCookieAuthenticator` reads the `restrainingIdPs` list from the execution config and
wraps the `AuthenticationFlowContext` in a `RestrainingAuthenticationFlowContext` before
delegating to Keycloak's built-in `CookieAuthenticator`. The wrapper overrides `success()`: if the
user is linked to a restraining IdP or carries a `pairwiseSub` attribute, it calls `attempted()`
instead, so the cookie step is treated as not fulfilled and the flow continues with the next
(alternative) execution — the real broker round trip. Otherwise it calls `success()` as usual.
