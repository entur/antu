package no.entur.antu.validator.id;

import java.util.Set;
import java.util.stream.Collectors;


public class TrainElementRegistryIdValidator implements ExternalReferenceValidator {

    public static final String NAME = "TrainElementRegistryIdValidator";
    private static final String TRAIN_ELEMENT_CODESPACE = "PEN";

    @Override
    public Set<IdVersion> validateReferenceIds(Set<IdVersion> externalIds) {
        return externalIds.stream().filter(TrainElementRegistryIdValidator::isValidTrainElementId).collect(Collectors.toSet());
    }

    private static boolean isValidTrainElementId(IdVersion idVersion) {
        String[] splittedId = idVersion.getId().split(":");
        return splittedId.length == 3 && TRAIN_ELEMENT_CODESPACE.equals(splittedId[0]);
    }

}
