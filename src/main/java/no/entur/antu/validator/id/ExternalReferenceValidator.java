package no.entur.antu.validator.id;

import java.util.Set;

public interface ExternalReferenceValidator {

    /**
     * Return a set of IDs that are valid according to this external reference validator.
     *
     * @return the IDs that were validated
     */
    Set<IdVersion> validateReferenceIds(Set<IdVersion> externalIdsToValidate);

}
