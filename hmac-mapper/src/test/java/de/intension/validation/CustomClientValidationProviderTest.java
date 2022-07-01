package de.intension.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator.PAIRWISE_FAILED_TO_GET_REDIRECT_URIS;
import static org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator.PAIRWISE_MALFORMED_SECTOR_IDENTIFIER_URI;
import static org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator.PAIRWISE_REDIRECT_URIS_MISMATCH;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.keycloak.common.Profile;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.validation.ValidationContext;
import org.keycloak.validation.ValidationError;
import org.keycloak.validation.ValidationResult;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;

class CustomClientValidationProviderTest
{

    private static final String SECTOR_IDENTIFIER = "http://a-static-url.de/sector_identifiers.json";

    /**
     * GIVEN: a valid hmac pairwise sub mapper
     * WHEN: validated by custom validator
     * THEN: no validation errors reported
     */
    @Test
    void should_not_have_validation_errors_when_config_is_valid()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        CustomClientValidationProvider customValidator = new CustomClientValidationProvider(mapper);

        ValidationResult validResult = new ValidationResult(Collections.<ValidationError>emptySet());
        ValidationResult result = customValidator.validate(prepareValidationContextMock(validResult, SECTOR_IDENTIFIER));

        assertEquals(0, result.getErrors().size());
    }

    /**
     * GIVEN: a hmac pairwise sub mapper
     * AND: mock object to return pairwise redirect uris mismatch validation error
     * WHEN: validated by custom validator
     * THEN: no validation errors reported
     */
    @Test
    void should_ignore_pairwise_redirect_uris_mismatch_validation_errors()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        CustomClientValidationProvider customValidator = new CustomClientValidationProvider(mapper);

        Set<ValidationError> validationResultSet = new HashSet<>();
        validationResultSet.add(new ValidationError("pairWise", "pairwise field", PAIRWISE_REDIRECT_URIS_MISMATCH, null));
        ValidationResult validResult = new ValidationResult(validationResultSet);
        ValidationResult result = customValidator.validate(prepareValidationContextMock(validResult, SECTOR_IDENTIFIER));

        assertEquals(0, result.getErrors().size());
    }

    /**
     * GIVEN: a valid hmac pairwise sub mapper
     * AND: mock object to return pairwise failed to get redirect uris validation error
     * WHEN: validated by custom validator
     * THEN: no validation errors reported
     */
    @Test
    void should_ignore_pairwise_failed_to_get_redirect_uris_validation_errors()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        CustomClientValidationProvider customValidator = new CustomClientValidationProvider(mapper);

        Set<ValidationError> validationResultSet = new HashSet<>();
        validationResultSet.add(new ValidationError("pairWise", "pairwise field", PAIRWISE_FAILED_TO_GET_REDIRECT_URIS, null));
        ValidationResult validResult = new ValidationResult(validationResultSet);
        ValidationResult result = customValidator.validate(prepareValidationContextMock(validResult, SECTOR_IDENTIFIER));

        assertEquals(0, result.getErrors().size());
    }

    /**
     * GIVEN: a valid hmac pairwise sub mapper
     * AND: mock object to return pairwise malformed sector identifier uri
     * WHEN: validated by custom validator
     * THEN: validation error reported for pairwise malformed sector identifier uri
     */
    @Test
    void should_report_validation_errors_when_not_redirect_uris_error()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        CustomClientValidationProvider customValidator = new CustomClientValidationProvider(mapper);

        Set<ValidationError> validationResultSet = new HashSet<>();
        validationResultSet.add(new ValidationError("pairWise", "pairwise field", PAIRWISE_MALFORMED_SECTOR_IDENTIFIER_URI, null));
        ValidationResult validResult = new ValidationResult(validationResultSet);
        ValidationResult result = customValidator.validate(prepareValidationContextMock(validResult, SECTOR_IDENTIFIER));

        assertEquals(1, result.getErrors().size());
        assertEquals(PAIRWISE_MALFORMED_SECTOR_IDENTIFIER_URI, result.getErrors().iterator().next().getLocalizedMessageKey());
    }

    /**
     * GIVEN: a hmac pairwise sub mapper without sector identifier
     * WHEN: validated by custom validator
     * THEN: validation error reported for missing sector identifier
     */
    @Test
    void should_report_validation_errors_on_missing_sector_identifier()
        throws Exception
    {
        HmacPairwiseSubMapper mapper = new HmacPairwiseSubMapper();
        CustomClientValidationProvider customValidator = new CustomClientValidationProvider(mapper);

        ValidationResult result = customValidator
            .validate(prepareValidationContextMock(new ValidationResult(Collections.<ValidationError>emptySet()), null));

        assertEquals(1, result.getErrors().size());
        assertEquals("pairwiseMissingSectorIdentifier", result.getErrors().iterator().next().getLocalizedMessageKey());
    }

    private ValidationContext<ClientModel> prepareValidationContextMock(ValidationResult validationResult, String sectorIdentifier)
        throws Exception
    {
        KeycloakSession session = mock(KeycloakSession.class);
        HttpClientProvider httpClientProvider = mock(HttpClientProvider.class);
        when(session.getProvider(HttpClientProvider.class)).thenReturn(httpClientProvider);
        InputStream inputStream = new ByteArrayInputStream(SECTOR_IDENTIFIER.getBytes());
        when(httpClientProvider.get(SECTOR_IDENTIFIER)).thenReturn(inputStream);

        ValidationContext<ClientModel> context = mock(ValidationContext.class);
        when(context.getSession()).thenReturn(session);
        when(context.toResult()).thenReturn(validationResult);
        Profile.getDisabledFeatures().add(Profile.Feature.AUTHORIZATION);
        ClientModel client = mock(ClientModel.class);
        when(client.getId()).thenReturn("1234");

        ProtocolMapperModel protocolMapperModel = createMapperModel("username", "HmacSHA256", "P5ZD+fqPLDTW", sectorIdentifier);
        Supplier<Stream<ProtocolMapperModel>> mapperModelStream = () -> Stream.of(protocolMapperModel);
        when(client.getProtocolMappersStream()).thenReturn(mapperModelStream.get(), mapperModelStream.get());
        when(context.getObjectToValidate()).thenReturn(client);
        RealmModel realm = mock(RealmModel.class);
        when(client.getRealm()).thenReturn(realm);
        CibaConfig cibaPolicy = mock(CibaConfig.class);
        when(realm.getCibaPolicy()).thenReturn(cibaPolicy);
        when(cibaPolicy.getBackchannelTokenDeliveryMode(client)).thenReturn(CibaConfig.CIBA_POLL_MODE);
        return context;
    }

    /**
     * Create Protocol mapper model with the local sub identifier passed
     * 
     * @param localSubIdentifier
     * @param hashAlgorithm
     * @param salt
     * @param sectorIdentifier
     * @return
     */
    private ProtocolMapperModel createMapperModel(String localSubIdentifier, String hashAlgorithm, String salt, String sectorIdentifier)
    {
        ProtocolMapperModel protocolMapperModel = new ProtocolMapperModel();
        protocolMapperModel.setConfig(new HashMap<String, String>());
        protocolMapperModel.setName("HMAC Mapper");
        Map<String, String> config = new HashMap<>();
        config.put("pairwiseSubHashAlgorithm", hashAlgorithm);
        config.put("pairwiseSubAlgorithmSalt", salt);
        config.put("pairwiseLocalSubIdentifier", localSubIdentifier);
        config.put(PairwiseSubMapperHelper.SECTOR_IDENTIFIER_URI, sectorIdentifier);
        protocolMapperModel.setConfig(config);
        protocolMapperModel.setProtocolMapper("oidc-hmac-pairwise-sub-mapper");
        return protocolMapperModel;
    }

}