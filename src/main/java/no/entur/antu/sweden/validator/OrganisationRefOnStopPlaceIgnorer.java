package no.entur.antu.sweden.validator;

import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.IdVersion;

import java.util.HashSet;
import java.util.Set;

/**
 * Mark references to organisation from a StopPlace as valid by default (this reference is not used in the data pipeline).
 */
public class OrganisationRefOnStopPlaceIgnorer implements ExternalReferenceValidator {

    @Override
    public Set<IdVersion> validateReferenceIds(Set<IdVersion> externalIdsToValidate) {
        Set<IdVersion> ignoredReferences = new HashSet<>(externalIdsToValidate);
        ignoredReferences.retainAll(isOfSupportedTypes(externalIdsToValidate));
        return ignoredReferences;
    }

    private Set<IdVersion> isOfSupportedTypes(Set<IdVersion> references) {
        Set<IdVersion> supportedTypes = new HashSet<>();
        for (IdVersion ref : references) {
            if (("OrganisationRef".equals(ref.getElementName()) && ref.getParentElementNames().contains("StopPlace"))) {
                supportedTypes.add(ref);
            }
        }
        return supportedTypes;
    }
}
