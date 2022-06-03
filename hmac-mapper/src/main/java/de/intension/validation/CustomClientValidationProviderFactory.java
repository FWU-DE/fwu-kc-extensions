package de.intension.validation;

import org.keycloak.models.KeycloakSession;
import org.keycloak.validation.ClientValidationProvider;
import org.keycloak.validation.ClientValidationProviderFactory;

/**
 * Custom client validation factory to Support {@link de.intension.protocol.oidc.mappers.HmacExtPairwiseSubMapper} feature.
 */
public class CustomClientValidationProviderFactory implements ClientValidationProviderFactory {

    private final ClientValidationProvider provider = new CustomClientValidationProvider();

    @Override
    public ClientValidationProvider create(KeycloakSession session) {
        return provider;
    }

    @Override
    public String getId() {
        return "custom-client-validation";
    }
}
