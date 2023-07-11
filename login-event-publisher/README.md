# Login event publisher

This extension listens to the LOGIN event in the Keycloak. After successfully listening the event it publishes the contents of the events to the RabbitMQ.

## Configurations

### Custom listener

To configure this event listener please configure the custom event listener in Keycloak as show in the image below.

<img src="event-listener-config.png" width="500" />

### Prerequisites

This extension depends on the Rabbitmq for publishing the event messages in the queue. Moreover, it also has a consumer spring boot application which is present on the gitlab repository.