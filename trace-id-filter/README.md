# Trace ID filter

JAX-RS request/response filter (`TraceIdFilter`) that assigns a trace ID to every request handled by
Keycloak and puts it into the logging context (MDC) under the key `trace-id`. This allows all log
entries belonging to one request, and to one authentication flow, to be correlated, also across
systems that pass the same ID along.

## How it works

For every request the trace ID is determined in this order:

1. the trace ID stored in the current authentication session (auth note `trace-id`), if any,
2. HTTP header `x-trace-id`,
3. query parameter `x-trace-id` (parameter name is matched case-insensitively),
4. otherwise a new ID is generated in the format `generated_<10 random letters>`, e.g.
   `generated_aoAgZPoADP`.

Supplied values are only accepted if they match `[A-Za-z0-9._:-]{1,128}`. Anything else (e.g.
values containing line breaks) is ignored and a new ID is generated, to prevent log injection.

To keep the trace ID consistent across the steps of an authentication flow, it is stored as auth
note `trace-id` in the authentication session:

* The request filter looks up the authentication session via the auth session cookie and the
  `tab_id` query parameter (or, if there is no `tab_id`, uses the only authentication session of
  the root session, e.g. on the broker endpoint). If the session already has a trace ID, that one
  wins over the request; otherwise the trace ID of the request is stored.
* The authentication session is usually created *during* the first request of a flow (e.g. the
  authorization endpoint), i.e. after the request filter ran. The response filter therefore stores
  the trace ID in a newly created authentication session before Keycloak commits the transaction.
  A trace ID that is only sent with the initial authorization request is thus kept for the whole
  login flow.

The response filter also removes the trace ID from the MDC, so it does not leak into log entries of
other requests processed later by the same worker thread.

Requests to the following paths are not processed (no trace ID in the MDC):

* `/realms/master` (and everything below)
* `/resources` (and everything below)

The filter has no configuration. It is registered automatically as JAX-RS `@Provider` when the jar
is deployed to `/opt/keycloak/providers` (followed by `kc.sh build`).

## Log format

The trace ID only shows up in the logs if the log format references the MDC key `trace-id` via
`%X{trace-id}`. Example for the format `<time> <thread> <trace ID> <log level> <message>`:

```
KC_LOG_CONSOLE_FORMAT="%d{yyyy-MM-dd HH:mm:ss,SSS} (%t) [%X{trace-id}] %-5p [%c] %s%e%n"
```

resulting in

```
2026-10-15 08:09:07,818 (executor-thread-1) [query-123] WARN  [org.keycloak.events] type="LOGIN_ERROR", realmName="fwu", ...
```

The same applies to `log-file-format` / `log-syslog-format` if file or syslog logging is used. For
JSON logging (`log-console-output=json`) the MDC is included automatically.

## Infrastructure

The WAF / reverse proxy in front of Keycloak must neither block nor strip the request header
`x-trace-id` and the query parameter `x-trace-id`.

## Data protection

* Trace IDs that mark authentication requests are personal data, as they can be used to link log
  entries to a login of a specific person.
* Trace IDs are only written to the system logs and to the (short-lived) authentication session.
  They are not stored in the database or in user attributes. Log entries are subject to the
  existing retention periods of the system logs.
* Trace IDs are used exclusively for error analysis.
* Supplied trace IDs are validated (character set and length) before they are logged.
