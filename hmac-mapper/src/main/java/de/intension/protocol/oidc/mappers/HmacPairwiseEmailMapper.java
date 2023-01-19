package de.intension.protocol.oidc.mappers;

import java.util.List;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.StringUtil;

/**
 * Pairwise identifier mapper using
 * <a href="https://datatracker.ietf.org/doc/html/rfc2104">HMAC</a>.
 * This OIDC mapper will replace the {@code email} field in the token with a
 * HMAC-hashed email
 * instead of the email. Sector identifier is mandatory for this mapper and must
 * have a valid hostname.
 *
 * @see <a href=
 *      "https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#mac-algorithms">mac-algorithms</a>
 */
public class HmacPairwiseEmailMapper extends HmacPairwiseSubMapper {

    public static final String PROVIDER_ID = "oidc-hmac-pairwise-email-mapper";

    private static final String EMAIL_DOMAIN_PROP_NAME = "emailDomain";
    private static final String EMAIL_DOMAIN_PROP_LABEL = "Email domain";
    private static final String EMAIL_DOMAIN_PROP_HELP = "The email domain is appended to the claim value after pseudonymization.";

    private static final String OVERRIDE_PROP_NAME = "override";
    private static final String OVERRIDE_PROP_LABEL = "Override existing";
    private static final String OVERRIDE_PROP_HELP = "Toggle overriding an existing email claim.";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "HMAC pairwise email";
    }

    @Override
    public String getHelpText() {
        return "Updates the email claim in tokens with a pseudonymized value from a selected attribute.";
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) {
            return token;
        }
        String localSub = getLocalIdentifierValue(userSession.getUser(), mappingModel);
        if (localSub == null) {
            return token;
        }
        token.setEmail(generateEmail(mappingModel, localSub));
        return token;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel,
            KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) {
            return token;
        }
        String localSub = getLocalIdentifierValue(userSession.getUser(), mappingModel);
        if (localSub == null) {
            return token;
        }
        token.setEmail(generateEmail(mappingModel, localSub));
        return token;
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel,
            KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        if (!OIDCAttributeMapperHelper.includeInUserInfo(mappingModel)) {
            return token;
        }
        String localSub = getLocalIdentifierValue(userSession.getUser(), mappingModel);
        if (localSub == null) {
            return token;
        }
        token.getOtherClaims().put("email", generateEmail(mappingModel, localSub));
        return token;
    }

    /**
     * Generate the HMAC identifier like in {@link HmacPairwiseSubMapper} but add an
     * email domain to it.
     */
    private String generateEmail(ProtocolMapperModel mappingModel, String email) {
        var pseudoEmail = generateIdentifier(mappingModel, getSectorIdentifier(mappingModel), email);
        var emailDomain = mappingModel.getConfig().get(EMAIL_DOMAIN_PROP_NAME);
        if (StringUtil.isBlank(emailDomain)) {
            pseudoEmail += "@" + email.split("@")[1];
        } else {
            pseudoEmail += "@" + emailDomain;
        }
        return pseudoEmail;
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalConfigProperties() {
        var configProperties = super.getAdditionalConfigProperties();
        configProperties.add(createEmailDomainConfig());
        return configProperties;
    }

    /**
     * Creates the mapper's configuration property for the email domain config to
     * use.
     */
    private static ProviderConfigProperty createEmailDomainConfig() {
        var property = new ProviderConfigProperty();
        property.setName(EMAIL_DOMAIN_PROP_NAME);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel(EMAIL_DOMAIN_PROP_LABEL);
        property.setHelpText(EMAIL_DOMAIN_PROP_HELP);
        return property;
    }
}
