package no.entur.antu.validator.id;

import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.IdVersion;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Validate that NeTEX references point to a valid stop or quay in the National Stop Place Registry.
 */
public class ReferenceToNsrValidator implements ExternalReferenceValidator {

    private final StopPlaceRepository stopPlaceRepository;

    public ReferenceToNsrValidator(StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceRepository = Objects.requireNonNull(stopPlaceRepository);
    }

    @Override
    public Set<IdVersion> validateReferenceIds(Set<IdVersion> externalIds) {
        Objects.requireNonNull(externalIds);
        Set<String> stopPlaceIds = stopPlaceRepository.getStopPlaceIds();
        Set<String> quayIds = stopPlaceRepository.getQuayIds();
        Set<IdVersion> validIds = new HashSet<>();

        for (IdVersion id : externalIds) {
            if (isValidQuayReference(quayIds, id) || isValidStopPlaceReference(stopPlaceIds, id)) {
                validIds.add(id);
            }
        }

        return validIds;
    }

    private boolean isValidStopPlaceReference(Set<String> stopPlaceIds, IdVersion id) {
        return id.getId().contains(":StopPlace:") && stopPlaceIds.contains(id.getId());
    }

    private boolean isValidQuayReference(Set<String> quayIds, IdVersion id) {
        return id.getId().contains(":Quay:") && quayIds.contains(id.getId());
    }

}
