package de.intension.rest;

public interface IKeycloakApiMapper {

    /**
     * Add user info attributes to resource (@{@link org.keycloak.broker.provider.BrokeredIdentityContext} or {@link org.keycloak.models.UserModel})
     */
    void addAttributesToResource(Object resource, String userInfo);

}
