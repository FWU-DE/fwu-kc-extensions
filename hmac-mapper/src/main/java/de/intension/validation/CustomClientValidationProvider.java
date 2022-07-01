package de.intension.validation;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator.PAIRWISE_FAILED_TO_GET_REDIRECT_URIS;
import static org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator.PAIRWISE_REDIRECT_URIS_MISMATCH;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.validation.DefaultClientValidationProvider;
import org.keycloak.validation.ValidationContext;
import org.keycloak.validation.ValidationError;
import org.keycloak.validation.ValidationResult;

import de.intension.protocol.oidc.mappers.HmacPairwiseSubMapper;

/**
 * This class is to filter the validation errors pairwise redirect uris mismatch and pairwise failed
 * to get redirect uris.
 */
public class CustomClientValidationProvider extends DefaultClientValidationProvider
{

    private static final String         PAIRWISE_FIELD_ID         = "pairWise";
    private static final List<String>   REDIRECT_ERRORS_TO_IGNORE = new LinkedList<>(Arrays.asList(PAIRWISE_REDIRECT_URIS_MISMATCH,
                                                                                                   PAIRWISE_FAILED_TO_GET_REDIRECT_URIS));

    private final HmacPairwiseSubMapper hmacPairwiseSubMapper;

    public CustomClientValidationProvider(HmacPairwiseSubMapper hmacPairwiseSubMapper)
    {
        this.hmacPairwiseSubMapper = hmacPairwiseSubMapper;
    }

    @Override
    public ValidationResult validate(ValidationContext<ClientModel> context)
    {
        ValidationResult result = null;
        try {
            ProtocolMapperRepresentation mapperRepresentation = getHmacPairwiseSubMapper(context);

            result = super.validate(context);

            //custom validation and error filter
            if (mapperRepresentation != null) {
                validateSectorIdentifier(mapperRepresentation);
                result = removeRedirectErrors(result);
            }
        } catch (ProtocolMapperConfigException e) {
            Set<ValidationError> validationErrors = new HashSet<>(result.getErrors());
            validationErrors.add(new ValidationError(PAIRWISE_FIELD_ID, e.getMessage(), e.getMessageKey(), null));
            result = new ValidationResult(validationErrors);

        }
        return result;
    }

    /**
     * In case of {@link HmacPairwiseSubMapper} the sectorIdentifier must not be null.
     */
    private void validateSectorIdentifier(ProtocolMapperRepresentation mapper)
        throws ProtocolMapperConfigException
    {
        String sectorIdentifier = PairwiseSubMapperHelper.getSectorIdentifierUri(mapper);
        hmacPairwiseSubMapper.validateSectorIdentifierNotEmpty(sectorIdentifier);
    }

    /**
     * Get protocol mapper representation of {@link HmacPairwiseSubMapper} from current context.
     */
    private ProtocolMapperRepresentation getHmacPairwiseSubMapper(ValidationContext<ClientModel> context)
    {
        ProtocolMapperRepresentation hmacExtMapper = null;
        List<ProtocolMapperRepresentation> foundPairwiseMappers = PairwiseSubMapperUtils
            .getPairwiseSubMappers(toRepresentation(context.getObjectToValidate(), context.getSession()));
        for (ProtocolMapperRepresentation mapper : foundPairwiseMappers) {
            if (hmacPairwiseSubMapper.getId().equals(mapper.getProtocolMapper())) {
                hmacExtMapper = mapper;
                break;
            }
        }
        return hmacExtMapper;
    }

    /**
     * Remove sectorIdentifier re-direct errors from {@link ValidationResult}
     */
    private ValidationResult removeRedirectErrors(ValidationResult result)
    {
        Set<ValidationError> errorsToKeep = new HashSet<>();
        if (result.fieldHasError(PAIRWISE_FIELD_ID)) {
            for (ValidationError error : result.getErrors()) {
                if (!(PAIRWISE_FIELD_ID.equals(error.getFieldId()) &&
                        REDIRECT_ERRORS_TO_IGNORE.contains(error.getLocalizedMessageKey()))) {
                    errorsToKeep.add(error);
                }
            }
        }
        return new ValidationResult(errorsToKeep);
    }

}