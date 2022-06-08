package de.intension.validation;

import de.intension.protocol.oidc.mappers.HmacExtPairwiseSubMapper;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.ProtocolMapperConfigException;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.utils.PairwiseSubMapperUtils;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.validation.*;

import java.util.*;

import static org.keycloak.models.utils.ModelToRepresentation.toRepresentation;
import static org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator.PAIRWISE_REDIRECT_URIS_MISMATCH;
import static org.keycloak.protocol.oidc.utils.PairwiseSubMapperValidator.PAIRWISE_FAILED_TO_GET_REDIRECT_URIS;

public class CustomClientValidationProvider extends DefaultClientValidationProvider {

    private final HmacExtPairwiseSubMapper hmacExtPairwiseSubMapper;

    public CustomClientValidationProvider(HmacExtPairwiseSubMapper hmacExtPairwiseSubMapper){
        this.hmacExtPairwiseSubMapper = hmacExtPairwiseSubMapper;
    }

    private static final String PAIRWISE_FIELD_ID = "pairWise";

    private static final List<String> REDIRECT_ERRORS_TO_IGNORE =
            new LinkedList<>(Arrays.asList(PAIRWISE_REDIRECT_URIS_MISMATCH,
                    PAIRWISE_FAILED_TO_GET_REDIRECT_URIS));

    @Override
    public ValidationResult validate(ValidationContext<ClientModel> context) {
        ValidationResult result = null;
        try {
            ProtocolMapperRepresentation mapperRepresentation = getHmacExtPairwiseSubMapper(context);

            result = super.validate(context);

            //custom validation and error filter
            if(mapperRepresentation != null){
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
     * In case of {@link HmacExtPairwiseSubMapper} the sectorIdentifier must not be null.
     */
    private void validateSectorIdentifier(ProtocolMapperRepresentation mapper) throws ProtocolMapperConfigException {
        String sectorIdentifier = PairwiseSubMapperHelper.getSectorIdentifierUri(mapper);
        hmacExtPairwiseSubMapper.validateSectorIdentifierNotEmpty(sectorIdentifier);
    }

    /**
     * Get protocol mapper representation of {@link HmacExtPairwiseSubMapper} from current context.
     */
    private ProtocolMapperRepresentation getHmacExtPairwiseSubMapper(ValidationContext<ClientModel> context) {
        ProtocolMapperRepresentation hmacExtMappers = null;
        List<ProtocolMapperRepresentation> foundPairwiseMappers = PairwiseSubMapperUtils.getPairwiseSubMappers(toRepresentation(context.getObjectToValidate(), context.getSession()));
        for(ProtocolMapperRepresentation mapper : foundPairwiseMappers){
            if(hmacExtPairwiseSubMapper.getId().equals(mapper.getProtocolMapper())){
                hmacExtMappers = mapper;
                break;
            }
        }
        return hmacExtMappers;
    }

    /**
     * Remove sectorIdentifier re-direct errors from {@link ValidationResult}
     */
    private ValidationResult removeRedirectErrors(ValidationResult result){
        Set<ValidationError> errorsToKeep = new HashSet<>();
        if(result.fieldHasError(PAIRWISE_FIELD_ID)){
            for(ValidationError error : result.getErrors()){
                if(!(PAIRWISE_FIELD_ID.equals(error.getFieldId()) &&
                        REDIRECT_ERRORS_TO_IGNORE.contains(error.getLocalizedMessageKey()))){
                    errorsToKeep.add(error);
                }
            }
        }
        return new ValidationResult(errorsToKeep);
    }

}
