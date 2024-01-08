package de.intension.events;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import de.intension.events.publishers.EventPublisher;
import de.intension.events.publishers.rabbitmq.RabbitMqEventPublisher;

/**
 * Factory for the user login event listener
 * 
 * @author kdeshpande
 */
public class LoginEventListenerProviderFactory
    implements EventListenerProviderFactory
{

    public static final String        PROVIDER_ID                    = "login-event-publisher";
    private static final Logger       logger                         = Logger.getLogger(LoginEventListenerProviderFactory.class);
    private EventPublisher            publisher;
    private DetailedLoginEventFactory eventFactory;

    @Override
    public EventListenerProvider create(KeycloakSession session)
    {
        return new LoginEventListenerProvider(session, publisher, eventFactory);
    }

    @Override
	public void init(Scope config) {
		publisher = new RabbitMqEventPublisher();
		this.publisher.initConnection(config);
        this.eventFactory = new DetailedLoginEventFactory(config);
		logger.info("init of login event provider factory completed successfully");
	}

    @Override
    public void postInit(KeycloakSessionFactory factory)
    {
        // Nothing to do here
    }

    // Call the publisher close method to close the connection and channel to
    // publisher
    @Override
    public void close()
    {
        this.publisher.close();
    }

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

}
