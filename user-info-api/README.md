# API definition for users metadata
This API-Module contains all data objects, user attributes and claims for users metadata

## Referenced by
This API is used by the following modules
* [user-info-provider](./../user-info-provider/README.md)
* [user-info-requester](./../user-info-requester/README.md)

## Standardized user attributes
The following attributes are used as an input for this mapper:

|Attribute Key|Type|Description|
|:----|:----|:----|
|heimatorganisation.id|String|UUID der Heimatorganisation (wird von Vidis vergeben)|
|heimatorganisation.name|String|Name der Heimatorganisation|
|heimatorganisation.bundesland|String|Bundesland|
|person.familienname|String|Familienname|
|person.vorname|String|Vorname|
|person.akronym|String|Akronym|
|person.geburtsdatum|String|Geburtsdatum|
|person.geschlecht|String|Geschlecht|
|person.lokalisierung|String|Lokalisierung|
|person.vertrauensstufe|String|Vertrauensstufe|

### Single context
|Attribute Key|Type|Description|
|:----|:----|:----|
|person.kontext.id|String|ID des Kontexts|
|person.kontext.org.id|String|ID der Organisation|
|person.kontext.org.kennung|String|Kenung der Schule (Schulidentifikator)|
|person.kontext.org.|String|Vidis Schulidentifikator|
|vidis_schulidentifikator| | |
|person.kontext.org.name|String|Name der Schule|
|person.kontext.org.typ|String|Typ der Organisation|
|person.kontext.rolle|String|Rolle der Person|
|person.kontext.status|String|Status|
### Multiple contexts

Context array
```
<zero> ::= 0
<posDigit> ::= 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
<digit> ::= <zero> | <posDigit>
<number> ::= <zero> <posDigit> | <posDigit> <digit>
```

| Attribute Key                        |Type|Description|
|:-------------------------------------|:----|:----|
| person.kontext.\<**number**>.id          |String|ID des Kontexts|
| person.kontext.\<**number**>.org.id      |String|ID der Organisation|
| person.kontext.\<**number**>.org.kennung |String|Kennung der Schule (Schulidentifikator)|
| person.kontext.\<**number**>.org.        |String|Vidis Schulidentifikator|
| vidis_schulidentifikator             | | |
| person.kontext.\<**number**>.org.name    |String|Name der Schule|
| person.kontext.\<**number**>.org.typ     |String|Typ der Organisation|
| person.kontext.\<**number**>.rolle       |String|Rolle der Person|
| person.kontext.\<**number**>.status      |String|Status|

## Standardized users metadata

|Field|Type|Description|Default|
|:----|:----|:----|:----|
|version|String|Version des Datenmodells bzw. der Schnittstelle| |
|pid|String|Subject-ID des Benutzers| |
|heimatorganisation.id|String|UUID der Heimatorganisation (wird von Vidis vergeben)| |
|heimatorganisation.name|String|Name der Heimatorganisation| |
|heimatorganisation.bundesland|ISO 3166-2:DE|Bundesland| |
| | |(e.g. DE-BY)| |
|person.name.familienname|String|Familienname|Fallback: last name aus den User Properties|
|person.name.vorname|String|Vorname|Fallback: first name aus den User Properties|
|person.name.akronym|String|Akronym in Kleinbuchstaben|Zusammengesetzt aus den ersten beiden Buchstaben von Vorname und Familienname|
|person.geburt.datum|String|Geburtstag| |
| |(YYYY-MM-DD)| | |
|person.geschlecht|ENUM|Geschlecht| |
|person.lokalisierung|RFC 5646|Lokalisierung|de-DE|
| |<ISO-639-1>-<ISO-3166>| | |
|person.vertrauensstufe|ENUM|Vertrauensstufe|VOLL|
|personenkontexte.ktid|String|ID des Kontexts|Hash: heimatorganisation.|
| | | |name +|
| | | |personenkontexte.|
| | | |organisation.|
| | | |kennung + personenkontexte.|
| | | |rolle|
|personenkontexte.organisation.orgid|String|ID der Organisation|Hash:|
| | | |personenkontexte.|
| | | |organisation.|
| | | |kennung + personenkontexte.rolle|
|personenkontexte.organisation.|String|Vidis Schulidentifikator|heimatorganisation.|
|vidis_schulidentifikator| | |name + personenkontexte.|
| | | |organisation.|
| | | |kennung (getrennt mit einem Punkt)|
|personenkontexte.organisation.|String|Name der Schule| |
|name| | | |
|personenkontexte.organisation.type|ENUM|Typ der Organisation|SCHULE|
|personenkontexte.rolle|ENUM|Rolle der Person| |
|personenkontexte.personenstatus|ENUM|Status|AKTIV|

