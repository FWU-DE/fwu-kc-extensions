package de.intension.protocol.oidc.mappers;

import java.util.List;

import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.*;
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
public class HmacPairwiseEmailMapper extends HmacPairwiseSubMapper
{

    public static final String  PROVIDER_ID             = "oidc-hmac-pairwise-email-mapper";

    private static final String EMAIL_DOMAIN_PROP_NAME  = "emailDomain";
    private static final String EMAIL_DOMAIN_PROP_LABEL = "Email domain";
    private static final String EMAIL_DOMAIN_PROP_HELP  = "The email domain is appended to the claim value after pseudonymization.";

    private static final String OVERRIDE_PROP_NAME      = "override";
    private static final String OVERRIDE_PROP_LABEL     = "Override existing";
    private static final String OVERRIDE_PROP_HELP      = "Toggle overriding an existing email claim.";

    @Override
    public String getId()
    {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType()
    {
        return "HMAC pairwise email";
    }

    @Override
    public String getHelpText()
    {
        return "Updates the email claim in tokens with a pseudonymized value from a selected attribute.";
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                    UserSessionModel userSession, ClientSessionContext clientSessionCtx)
    {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) {
            return token;
        }
        UserModel user = userSession.getUser();
        String localSub = HmacPairwiseSubMapperHelper.getLocalIdentifierValue(user, mappingModel);
        if (!checkPrerequisites(localSub, mappingModel, token.getEmail())) {
            return token;
        }
        token.setEmail(generateEmail(mappingModel, localSub, user.getEmail()));
        return token;
    }

    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel,
                                            KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx)
    {
        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) {
            return token;
        }
        UserModel user = userSession.getUser();
        String localSub = HmacPairwiseSubMapperHelper.getLocalIdentifierValue(user, mappingModel);
        if (!checkPrerequisites(localSub, mappingModel, token.getEmail())) {
            return token;
        }
        token.setEmail(generateEmail(mappingModel, localSub, user.getEmail()));
        return token;
    }

    @Override
    public AccessToken transformUserInfoToken(AccessToken token, ProtocolMapperModel mappingModel,
                                              KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx)
    {
        if (!OIDCAttributeMapperHelper.includeInUserInfo(mappingModel)) {
            return token;
        }
        UserModel user = userSession.getUser();
        String localSub = HmacPairwiseSubMapperHelper.getLocalIdentifierValue(user, mappingModel);
        if (!checkPrerequisites(localSub, mappingModel,
                                String.valueOf(token.getOtherClaims().get("email")))) {
            return token;
        }
        token.getOtherClaims().put("email", generateEmail(mappingModel, localSub, user.getEmail()));
        return token;
    }

    /**
     * Checks whether to execute the mapper.
     */
    private boolean checkPrerequisites(String localSub, ProtocolMapperModel mappingModel, String email)
    {
        if (localSub == null) {
            return false;
        }
        boolean overrideEmail = Boolean.parseBoolean(mappingModel.getConfig().get(OVERRIDE_PROP_NAME));
        return overrideEmail || email == null;
    }

    /**
     * Generate the HMAC identifier like in {@link HmacPairwiseSubMapper} but add an
     * email domain to it.
     */
    private String generateEmail(ProtocolMapperModel mappingModel, String localSub, String email)
    {
        var pseudoEmail = HmacPairwiseSubMapperHelper.generateIdentifier(mappingModel, localSub);
        var emailDomain = mappingModel.getConfig().get(EMAIL_DOMAIN_PROP_NAME);
        if (StringUtil.isBlank(emailDomain)) {
            if (!ObjectUtil.isBlank(email)) {
                pseudoEmail += "@" + email.split("@")[1];
            }
        }
        else {
            pseudoEmail += "@" + emailDomain;
        }
        return pseudoEmail;
    }

    @Override
    public List<ProviderConfigProperty> getAdditionalConfigProperties()
    {
        var configProperties = super.getAdditionalConfigProperties();
        configProperties.add(createEmailDomainConfig());
        configProperties.add(createOverrideConfig());
        return configProperties;
    }

    private ProviderConfigProperty createOverrideConfig()
    {
        var property = new ProviderConfigProperty();
        property.setName(OVERRIDE_PROP_NAME);
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setLabel(OVERRIDE_PROP_LABEL);
        property.setHelpText(OVERRIDE_PROP_HELP);
        property.setDefaultValue(true);
        return property;
    }

    private static ProviderConfigProperty createEmailDomainConfig()
    {
        var property = new ProviderConfigProperty();
        property.setName(EMAIL_DOMAIN_PROP_NAME);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setLabel(EMAIL_DOMAIN_PROP_LABEL);
        property.setHelpText(EMAIL_DOMAIN_PROP_HELP);
        return property;
    }
}
