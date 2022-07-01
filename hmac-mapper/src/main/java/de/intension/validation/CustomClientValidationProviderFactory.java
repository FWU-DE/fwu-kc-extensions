package de.intension.validation;

import org.keycloak.models.KeycloakSession;
import org.keycloak.validation.ClientValidationProvider;
import org.keycloak.validation.ClientValidationProviderFactory;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;

/**
 * Custom client validation factory to Support
 * {@link de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper} feature.
 */
public class CustomClientValidationProviderFactory
    implements ClientValidationProviderFactory
{

    private final HmacPairwiseSubMapper    hmacPairwiseSubMapper = new HmacPairwiseSubMapper();
    private final ClientValidationProvider provider              = new CustomClientValidationProvider(hmacPairwiseSubMapper);

    @Override
    public ClientValidationProvider create(KeycloakSession session)
    {
        return provider;
    }

    @Override
    public String getId()
    {
        return "custom-client-validation";
    }
}