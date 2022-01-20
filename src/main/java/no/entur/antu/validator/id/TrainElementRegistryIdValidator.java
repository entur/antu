package no.entur.antu.validator.id;

import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.IdVersion;

import java.util.Set;
import java.util.stream.Collectors;


/**
 * Validate that TrainElement objects refer to the PEN codespace (Entur-plass).
 */
public class TrainElementRegistryIdValidator implements ExternalReferenceValidator {

    public static final String NAME = "TrainElementRegistryIdValidator";
    private static final String TRAIN_ELEMENT_CODESPACE = "PEN";

    @Override
    public Set<IdVersion> validateReferenceIds(Set<IdVersion> externalIds) {
        return externalIds.stream().filter(TrainElementRegistryIdValidator::isValidTrainElementId).collect(Collectors.toSet());
    }

    private static boolean isValidTrainElementId(IdVersion idVersion) {
        String[] idParts = idVersion.getId().split(":");
        return idParts.length == 3 && TRAIN_ELEMENT_CODESPACE.equals(idParts[0]);
    }

}
