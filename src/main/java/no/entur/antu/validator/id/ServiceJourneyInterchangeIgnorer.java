package no.entur.antu.validator.id;

import java.util.HashSet;
import java.util.Set;

public class ServiceJourneyInterchangeIgnorer implements ExternalReferenceValidator {

    @Override
    public Set<IdVersion> validateReferenceIds(Set<IdVersion> externalIdsToValidate) {

        // All references of supported type should be returned as validated

        Set<IdVersion> ignoredReferences = new HashSet<>(externalIdsToValidate);
        ignoredReferences.retainAll(isOfSupportedTypes(externalIdsToValidate));

        return ignoredReferences;

    }

    private Set<IdVersion> isOfSupportedTypes(Set<IdVersion> references) {

        Set<IdVersion> supportedTypes = new HashSet<>();

        for (IdVersion ref : references) {
            if ("FromPointRef".equals(ref.getElementName()) && ref.getId().contains("ScheduledStopPoint")) {
                supportedTypes.add(ref);
            } else if ("FromJourneyRef".equals(ref.getElementName()) && ref.getId().contains("ServiceJourney")) {
                supportedTypes.add(ref);
            }
        }

        return supportedTypes;
    }

}
