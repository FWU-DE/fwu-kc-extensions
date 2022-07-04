package de.intension.protocol.oidc.mappers;

import java.util.List;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Interface to "adopt" the {@link org.keycloak.protocol.oidc.mappers.AbstractPairwiseSubMapper}
 * behaviour.
 * Should make it easier to identify code changes after a new Keycloak release.
 */
public interface IPairwiseSubMapper
    extends OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper
{

    String getIdPrefix();

    /**
     * Generates a pairwise subject identifier.
     */
    String generateSub(ProtocolMapperModel mappingModel, String sectorIdentifier, String localSub);

    /**
     * Implement to add additional provider configuration properties.
     * By default, a pairwise sub mapper will only contain configuration for a sector identifier
     * URI.
     */
    List<ProviderConfigProperty> getAdditionalConfigProperties();

    /**
     * Implement to add additional configuration validation.
     * Called when instance of mapperModel is created/updated for this protocolMapper through admin
     * endpoint.
     */
    void validateAdditionalConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer, ProtocolMapperModel mapperModel)
        throws ProtocolMapperConfigException;

    /**
     * Implement to validate the SECTOR_IDENTIFIER_URI.
     * This method is inspired by
     * {@link org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator#validate(KeycloakSession, ClientModel, ProtocolMapperModel)}
     */
    void validateSectorIdentifier(KeycloakSession session, ClientModel client, ProtocolMapperModel mapperModel)
        throws ProtocolMapperConfigException;
}
