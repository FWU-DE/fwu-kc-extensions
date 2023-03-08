package de.intension.protocol.oidc.mappers;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

/**
 * Pairwise identifier mapper using
 * <a href="https://datatracker.ietf.org/doc/html/rfc2104">HMAC</a>.
 * This OIDC mapper will add the {@code pseudonyms} field in the token with a list of Pseudonyms
 * from
 * the {@link HmacPairwiseSubMapper} of the configured Clients. Sector identifier is mandatory for
 * this mapper.
 * The hostname of the sectorIdentifier is used in the hash
 *
 * @see <a href=
 *      "https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#mac-algorithms">mac-algorithms</a>
 */
public class HmacPairwisePseudonymListMapper extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper
{
    protected static final String CLIENT_DOES_NOT_EXIST_MSG_KEY = "noConfigForClientFoundOrClientDoesNotExist";
    protected static final String WRONG_MAPPER_TYPE_MSG_KEY = "wrongMapperType";
    protected static final String TARGET_CLAIM_NOT_SET_MSG_KEY = "targetClaimNotSetForPseudonymListMapper";
    protected static final String       CLAIM_PROP_NAME       = "pseudonymListClaimName";
    protected static final String       CLAIM_PROP_HELP       = "Which claim should hold the pseudonym list";
    protected static final String       CLAIM_PROP_LABEL      = "Target claim for pseudonym list";

    protected static final String       CLIENTS_PROP_NAME     = "clients";
    protected static final String       CLIENTS_PROP_HELP     = "List of clients to retrieve and add Pseudonyms for";
    protected static final String       CLIENTS_PROP_LABEL    = "Clients";
    protected static final String ACCEPTED_MAPPER_TYPE = "client mapper";

    private static final Logger LOG = Logger.getLogger(HmacPairwisePseudonymListMapper.class);

    @Override
    public String getId()
    {
        return "oidc-hmac-pairwise-subject-list-mapper";
    }

    @Override
    public String getDisplayCategory()
    {
        return AbstractOIDCProtocolMapper.TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getDisplayType()
    {
        return "List of HMAC Pairwise subject with static sectorIdentifier";
    }

    @Override
    public String getHelpText()
    {
        return "Calculates list of pseudonyms using 'HMAC Pairwise subject with static sectorIdentifier'";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties()
    {
        List<ProviderConfigProperty> configProperties = new LinkedList<>();
        configProperties.add(createClientListConfig());
        configProperties.add(createClaimNameConfig());
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, this.getClass());
        return configProperties;
    }

    /**
     * Creates the mapper's configuration property for the client list
     *
     * @return Config property item
     */
    private ProviderConfigProperty createClientListConfig()
    {
        var property = new ProviderConfigProperty();
        property.setName(CLIENTS_PROP_NAME);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel(CLIENTS_PROP_LABEL);
        property.setHelpText(CLIENTS_PROP_HELP);
        return property;
    }

    private ProviderConfigProperty createClaimNameConfig()
    {
        var property = new ProviderConfigProperty();
        property.setName(CLAIM_PROP_NAME);
        property.setLabel(CLAIM_PROP_LABEL);
        property.setHelpText(CLAIM_PROP_HELP);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        return property;
    }

    @Override
    public void validateConfig(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel mapperContainer,
                               ProtocolMapperModel mapperModel)
        throws ProtocolMapperConfigException
    {
        if (mapperContainer instanceof ClientModel) {
            validateAllClientConfigsExist(session, mapperModel);
            validateClaimIsConfigured(mapperModel);
        } else {
            throw new ProtocolMapperConfigException(WRONG_MAPPER_TYPE_MSG_KEY, "PseudonymlistMapper can only work on Client Mappers.", ACCEPTED_MAPPER_TYPE);
        }
    }

    /**
     * Validate that all of the configured Clients have a config for the 'HMAC Pairwise subject with static sectorIdentifier' Mapper
     * @param session the current keycloaksession
     * @param mapperModel containing config fur this mapper
     * @throws ProtocolMapperConfigException if one of the client has no 'HMAC Pairwise subject with static sectorIdentifier' Mapper
     */
    private void validateAllClientConfigsExist(KeycloakSession session, ProtocolMapperModel mapperModel)
        throws ProtocolMapperConfigException
    {
        Set<String> clients = getClients(mapperModel);
        for (String clientId : clients) {
            if (getProtocolMapperModelForClient(session, clientId).isEmpty()) {
                throw new ProtocolMapperConfigException(CLIENT_DOES_NOT_EXIST_MSG_KEY, "PseudonymListMapper config not saved. Client list contains invalid client", clientId);
            }
        }
    }

    private void validateClaimIsConfigured(ProtocolMapperModel mapperModel) throws ProtocolMapperConfigException {
        if (mapperModel.getConfig().get(CLAIM_PROP_NAME) == null || mapperModel.getConfig().get(CLAIM_PROP_NAME).isBlank() ) {
            throw new ProtocolMapperConfigException("PseudonymlistmapperConfig could not be saved. Target Claim required.", TARGET_CLAIM_NOT_SET_MSG_KEY);
        }

    }

    private static Set<String> getClients(ProtocolMapperModel mapperModel)
    {
        return Stream.of(mapperModel.getConfig().get(CLIENTS_PROP_NAME).split("#{2}|,")).map(String::trim).collect(Collectors.toSet());
    }

    private static Optional<ProtocolMapperModel> getProtocolMapperModelForClient(KeycloakSession session, String clientId)
    {
        return Optional.ofNullable(session.getContext().getRealm().getClientByClientId(clientId))
            .flatMap(clientModel -> clientModel.getProtocolMappersStream()
                .filter(mapper -> mapper.getProtocolMapper().equals(HmacPairwiseSubMapper.PROTOCOL_MAPPER_ID))
                .findAny());
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                    UserSessionModel userSession,
                                    ClientSessionContext clientSessionCtx)
    {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) {
            return token;
        }
        generatePseudonymListClaim(token, mappingModel, session, userSession.getUser());
        return token;
    }

    /**
     * generate PseudonymListClaim or append pseudonyms to existing claim (claim must be of type Map(String,String) so append works)
     * @param token Token to manipulate Access and ID token are accepted
     * @param mappingModel mapperModel containing configuration for mapper
     * @param session current keycloak-session
     * @param user user for which the pseudonyms should be creted
     */
    private void generatePseudonymListClaim(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserModel user)
    {
        for (String client : getClients(mappingModel)) {
            Optional<ProtocolMapperModel> protocolMapperModelForClient = getProtocolMapperModelForClient(session, client);
            if (protocolMapperModelForClient.isEmpty()) {
                LOG.warnf("Could not find HMACPairwiseSubMapperConfig for client %s of PseudonymListMapper(%s) Skipping Client", client, mappingModel.getName());
                continue;
            }

            String localSub = HmacPairwiseSubMapperHelper.getLocalIdentifierValue(user, protocolMapperModelForClient.get());
            if (localSub == null) {
                return;
            }
            addPseudonymToTokenClaim(token, mappingModel.getConfig().get(CLAIM_PROP_NAME), client, HmacPairwiseSubMapperHelper
                    .generateIdentifier(protocolMapperModelForClient.get(), localSub));
        }
    }

    /**
     * Add pseudonym to {@link IDToken}, {@link AccessToken} or UserInfoToken claim holding pseudonym
     * map.
     *
     * @param token     Token to extend
     * @param claim     the claim in which the client-pseudonym map should be stored.
     * @param clientId  clientId to which the pseudonym belongs
     * @param pseudonym generated Pairwise hmac subject identifier
     */
    private void addPseudonymToTokenClaim(IDToken token, String claim, String clientId, String pseudonym)
    {
        Map<String, String> pseudonyms = (Map<String, String>)token.getOtherClaims().get(claim);
        if (pseudonyms == null) {
            pseudonyms = new HashMap<>();
        }
        pseudonyms.put(clientId, pseudonym);
        token.getOtherClaims().put(claim, pseudonyms);
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel,
                                            KeycloakSession session, UserSessionModel userSession,
                                            ClientSessionContext clientSessionCtx)
    {
        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) {
            return token;
        }
        generatePseudonymListClaim(token, mappingModel, session, userSession.getUser());
        return token;
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel,
                                              KeycloakSession session, UserSessionModel userSession,
                                              ClientSessionContext clientSessionCtx)
    {
        if (!OIDCAttributeMapperHelper.includeInUserInfo(mappingModel)) {
            return token;
        }

        generatePseudonymListClaim(token, mappingModel, session, userSession.getUser());
        return token;
    }
}
