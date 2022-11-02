# Whitelist Authenticator Schools

## Configuration

### Preparing a schools whitelist (JSON)

The Whitelist must be stored as a simple JSON-File on a HTTP-Server.

**Structure:**

List of JSON-Objects having two attributes
1. `spAlias`: Service Provider Alias from Keycloak client configuration
2. `listOfSchools`: JSON-Array with School IDs

**Example:**
```
[
    {
        "spAlias": "client01",
        "listOfSchools": ["817","912"]
    },
    {
        "spAlias": "client02",
        "listOfSchools": ["817","912","421"]
    }
]
```

### Execution step Configuration

<img src="../docs/whitelist_schools/wl_schools_execution_config.png" width="70%"/>

| Field                            | Description                                                                           |
|----------------------------------|---------------------------------------------------------------------------------------|
| User attribute                   | User attribute which contains the school ID information send by the Identity Provider |
| Whitelist URI                    | Reference to the Whitelist configuration file (JSON format)                           |
| Cache refresh Interval (minutes) | Defines the refresh interval for the internal whitelist configuration cache           |

### Authentication Flow Configuration

<img src="../docs/whitelist_schools/authentication_flow.png" width="70%"/>

1. Copy or modify existing Authentication Flow
2. Add new execution step with `Add Execution` button
3. Select `Whitelist Authenticator For Schools` Provider
3. Configure Provider via `Actions -> Config`
4. Add execution step to the very end and set it to `REQUIRED`

