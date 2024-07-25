# License Connect Authenticator

## Configuration

### SPI environment variables

The following environment variables must be set within the deployment pipeline.

| Property                                                           | Description                        | Example                                    |
|--------------------------------------------------------------------|------------------------------------|--------------------------------------------|
| KC_SPI_AUTHENTICATOR_LICENSE_CONNECT_AUTHENTICATOR_LICENSE_URL     | URI for the license connect        | http://mockserver:1080/v1/licences/request |
| KC_SPI_AUTHENTICATOR_LICENSE_CONNECT_AUTHENTICATOR_LICENSE_API_KEY | API key used to conenct to the API | sample-api-key                             |

### Fetching the user license

The license associated with user is managed by a microservice, which provides endpoints to gather those information.

**Endpoint:** <hostname>/v1/licenses/request

Request body

```json
{
  "userId": "02e71a9d-d68d-3050-9a0d-5b963c06aec0",
  "clientId": "Angebot12345",
  "schulkennung": "DE-MV-SN-51201",
  "bundesland": "DE-MV"
}
```

Response body

```json
{
  "hasLicences": true,
  "licences": [
    {
      "license_code": "VHT-9234814-fk68-acbj6-3o9jyfilkq2pqdmxy0j"
    },
    {
      "license_code": "COR-3rw46a45-345c-4237-a451-4333736ex015"
    }
  ]
}
```

## Behaviour

### Reading of configuration

The mentioned REST-Endpoint will be called during each login.

### Permitting or denying

If the user tries to login to a specific client and

* the user has license associated and returned as response in the REST-API then the login is  _permitted_ and attribute named `license` is added to the user.
* the user does not have license associated and REST-API returns 404,500 or non successful response then the login is not _permitted_.
