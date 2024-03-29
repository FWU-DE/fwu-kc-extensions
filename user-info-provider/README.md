# Vidis Info Mapper
Service Provider Mapper, which reads standard metadata attributes from user and adds them to the token.
Output is a standardized userInfo JSON-Structure.

## Input
This mapper uses the following standardized user attributes as an input:
[user-info-api](./../user-info-api/README.md)

## Configuration
If a Service Provider supports processing users metadata, a new mapper has to be configured for it.
1. Go to "Clients" and select the client to configure
2. Go to Tab "Mappers" and click on Button "create" adding a new mapper
3. Select "Vidis Info Mapper" as mapper type
4. De-/Activate fields based on the data privacy agreement
5. Set "Token Claim Name" for the metada data JSON structure
6. Define which token should contain this data

<img src="../docs/userinfo/Vidis-Info-Mapper.png" width="70%"/>

## Output
This mapper produces the following standardized metadata JSON-structure
[user-info-api](./../user-info-api/README.md)

### Example JSON - Users metadata
```json
{
  "version": "1.0.0",
  "pid": "d3febc7d-14ed-323f-b361-34d07527cdc0",
  "heimatorganisation": {
    "id": "DE-SN-Schullogin",
    "name": "Schulischer Anmeldeservice Musterstadt",
    "bundesland": "DE-BY"
  },
  "person": {
    "name": {
      "familienname": "Muster",
      "vorname": "Max",
      "akronym": "mamu",
      "initialenFamilienname": "M",
      "initialenVorname": "M"
    },
    "geburt": {
      "datum": "2010-01-01",
      "alter": "12",
      "volljaehrig": "NEIN",
      "geburtsort": "Ostfildern, Deutschland"
    },
    "geschlecht": "D",
    "lokalisierung": "de-DE",
    "vertrauensstufe": "VOLL"
  },
  "personenkontexte": [
    {
      "id": "af3a88fc-d766-11ec-9d64-0242ac120002",
      "organisation": {
        "orgid": "15685758-d18e-49c1-a644-f9996eb0bf08",
        "kennung": "NI_12345",
        "name": "Muster-Schule",
        "typ": "SCHULE",
        "vidis_schulidentifikator": "de-sn-schullogin.ni_12345"
      },
      "rolle": "LERN",
      "personenstatus": "AKTIV",
      "gruppen": [
        {
          "gruppe": {
            "id": "ab34d607-b950-41a5-b69d-80b8812c224a",
            "mandant": "02feb60dc3f691af4a4bf92410fac8292bb8e7d6adebb70b2a65d3c35d825d8a",
            "orgid": "02feb60dc3f691af4a4bf92410fac8292bb8e7d6adebb70b2a65d3c35d825d8a",
            "referrer": "fe4e50cb-c148-4156-8c2f-dc5260b267cf",
            "bezeichnung": "Englisch, 2. Klasse",
            "thema": "Thema",
            "beschreibung": "Beschreibung der Gruppe",
            "typ": "SONSTIG",
            "bereich": "WAHL",
            "optionen": [
              "01",
              "02"
            ],
            "differenzierung": "G",
            "bildungsziele": [
              "GS"
            ],
            "jahrgangsstufen": [
              "JS_02"
            ],
            "faecher": [
              {
                "code": "EN"
              }
            ],
            "referenzgruppen": [
              {
                "id": "21252996-7a5d-47b5-9c62-c416460908f0",
                "rollen": [
                  "LERN",
                  "LEHR"
                ]
              }
            ],
            "laufzeit": {
              "von": "2023-08-01",
              "bis": "2024-01-31",
              "sichtfreigabe": "JA"
            },
            "revision": "1"
          },
          "gruppenzugehoerigkeit": {
            "rollen": [
              "LEHR"
            ]
          }
        }
      ],
      "loeschung": {
        "zeitpunkt": "2099-12-31T23:59Z"
      }
    },
    {
      "id": "af3a88fc-d766-11ec-9d64-0242ac112345",
      "organisation": {
        "orgid": "15685758-d18e-49c1-a644-f9996e12345",
        "kennung": "0972",
        "name": "Gymnasium",
        "typ": "SCHULE",
        "vidis_schulidentifikator": "de-sn-schullogin.0972"
      },
      "rolle": "LEHR",
      "personenstatus": "AKTIV",
      "loeschung": {
        "zeitpunkt": "2099-12-31T23:59Z"
      }
    }
  ]
}
```

