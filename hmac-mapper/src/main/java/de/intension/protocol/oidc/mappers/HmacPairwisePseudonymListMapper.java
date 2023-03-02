package de.intension.protocol.oidc.mappers;

import java.util.*;

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
    private static final Logger LOG = Logger.getLogger(HmacPairwisePseudonymListMapper.class);

    private static final String CLIENT_DOES_NOT_EXIST = "noConfigForClientFoundOrClientDoesNotExist";
    private static final String       CLAIM_PROP_NAME       = "pseudonymListClaimName";
    private static final String       CLAIM_PROP_HELP       = "Which claim should hold the pseudonym list";
    private static final String       CLAIM_PROP_LABEL      = "Target claim for pseudonym list";

    private static final String       CLIENTS_PROP_NAME     = "clients";
    private static final String       CLIENTS_PROP_HELP     = "List of clients to retrieve and add Pseudonyms for";
    private static final String       CLIENTS_PROP_LABEL    = "Clients";

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
        return "Calculates list of pseudonyms using HMACPaiwrwiseSubMapper";
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
        }
    }

    private void validateAllClientConfigsExist(KeycloakSession session, ProtocolMapperModel mapperModel)
        throws ProtocolMapperConfigException
    {
        List<String> clients = getClients(mapperModel);
        for (String clientId : clients) {

            if (getProtocollMapperModelForClient(session, clientId).isEmpty()) {
                throw new ProtocolMapperConfigException(CLIENT_DOES_NOT_EXIST, CLIENT_DOES_NOT_EXIST, clientId);
            }
        }
    }

    private static List<String> getClients(ProtocolMapperModel mapperModel)
    {
        return Arrays.asList(mapperModel.getConfig().get(CLIENTS_PROP_NAME).split("#{2}|,"));
    }

    private static Optional<ProtocolMapperModel> getProtocollMapperModelForClient(KeycloakSession session, String clientId)
    {
        return Optional.ofNullable(session.getContext().getRealm().getClientByClientId(clientId.trim()))
            .flatMap(clientModel -> clientModel.getProtocolMappersStream()
                .filter(mapper -> mapper.getProtocolMapper().equals(HmacPairwiseSubMapper.PROTOCOLL_MAPPER_ID))
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

    private void generatePseudonymListClaim(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserModel user)
    {
        for (String client : getClients(mappingModel)) {
            Optional<ProtocolMapperModel> protocollMapperModelForClient = getProtocollMapperModelForClient(session, client);
            if (protocollMapperModelForClient.isEmpty()) {
                LOG.warnf("Could not find HMACPairwiseSubMapperConfig for client %s. Skipping Client", client);
                continue;
            }

            String localSub = HmacPairwiseSubMapperHelper.getLocalIdentifierValue(user, protocollMapperModelForClient.get());
            if (localSub == null) {
                return;
            }
            addPseudonymToTokenClaim(token, mappingModel.getConfig().get(CLAIM_PROP_NAME), client, HmacPairwiseSubMapperHelper
                .generateIdentifier(protocollMapperModelForClient.get(), localSub));
        }
    }

    /**
     * Add pseudonym to {@link IDToken}, {@link AccessToken} or UserInfoToken claim holding pseudonym
     * list.
     *
     * @param token     Token to extend
     * @param claim     the claim in which the client-pseudonym map should be stored.
     * @param clientId  clientId to which the pseudonym belongs
     * @param pseudonym generated Pairwise hmac subject identifier
     */
    protected void addPseudonymToTokenClaim(IDToken token, String claim, String clientId, String pseudonym)
    {
        Map<String, String> pseudonyms = (Map<String, String>)token.getOtherClaims().get(claim);
        if (pseudonyms == null) {
            pseudonyms = new HashMap<>();
        }
        pseudonyms.put(clientId, pseudonym);
        token.getOtherClaims().put(claim, pseudonyms);
    }
}
