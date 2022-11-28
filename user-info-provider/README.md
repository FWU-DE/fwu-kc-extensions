# User Info Mapper
Service Provider Mapper, which reads standard metadata attributes from user and adds them to the token.
Output is a standardized userInfo JSON-Structure.

## Input
This mapper uses the following standardized user attributes as an input:
[user-info-api](./../user-info-api/README.md)

## Configuration
If a Service Provider supports processing users metadata, a new mapper has to be configured for it.
1. Go to "Clients" and select the client to configure
2. Go to Tab "Mappers" and click on Button "create" adding a new mapper
3. Select "User Info Mapper" as mapper type
4. De-/Activate fields based on the data privacy agreement
5. Set "Token Claim Name" for the metada data JSON structure
6. Define which token should contain this data

<img src="../docs/userinfo/User-Info-Mapper.png" width="70%"/>

## Output
This mapper produces the following standardized metadata JSON-structure
[user-info-api](./../user-info-api/README.md)

### Example JSON - Users metadata
```json
{
    "version": "1.0.0",
    "pid": "d3febc7d-14ed-323f-b361-34d07527cdc0",
    "heimatorganisation": {
        "id": "3e3b679c-6cae-11ed-a1eb-0242ac120002",
        "name": "DE-SN-Schullogin",
        "bundesland": "DE-BY"
    },
    "person": {
        "name": {
            "familienname": "Muster",
            "vorname": "Max",
            "akronym": "mamu"
        },
        "geburt": {
            "datum": "2010-01-01"
        },
        "geschlecht": "D",
        "lokalisierung": "de-DE",
        "vertrauensstufe": "VOLL"
    },
    "personenkontexte": [
        {
            "ktid": "af3a88fc-d766-11ec-9d64-0242ac120002",
            "organisation": {
                "orgid": "15685758-d18e-49c1-a644-f9996eb0bf08",
                "name": "Muster-Schule",
                "typ": "SCHULE",
                "vidis_schulidentifikator": "de-sn-schullogin.ni_12345"
            },
            "rolle": "LERN",
            "personenstatus": "AKTIV"
        },
        {
            "ktid": "af3a88fc-d766-11ec-9d64-0242ac112345",
            "organisation": {
                "orgid": "15685758-d18e-49c1-a644-f9996e12345",
                "name": "Gymnasium",
                "typ": "SCHULE",
                "vidis_schulidentifikator": "de-sn-schullogin.0972"
            },
            "rolle": "LEHR",
            "personenstatus": "AKTIV"
        }
    ]
}
```

