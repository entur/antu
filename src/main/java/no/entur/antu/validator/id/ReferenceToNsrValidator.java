package no.entur.antu.validator.id;

import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validate that NeTEX references point to a valid element type.
 */
public class ReferenceToNsrValidator {

    private static final String MESSAGE_FORMAT_UNRESOLVED_EXTERNAL_REFERENCE = "Unresolved reference to external reference data";
    private final StopPlaceRepository stopPlaceRepository;

    public ReferenceToNsrValidator(StopPlaceRepository stopPlaceRepository) {
        this.stopPlaceRepository = stopPlaceRepository;
    }

    public List<ValidationReportEntry> validate(List<IdVersion> localRefs) {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
        Set<String> stopPlaceIds = stopPlaceRepository.getStopPlaceIds();
        Set<String> quayIds = stopPlaceRepository.getQuayIds();

        for (IdVersion id : localRefs) {
            if (id.getId().contains(":Quay:") && !quayIds.contains(id.getId())) {
                validationReportEntries.add(createValidationReportEntry(id));
            } else if (id.getId().contains(":StopPlace:") && !stopPlaceIds.contains(id.getId())) {
                validationReportEntries.add(createValidationReportEntry(id));
            }
        }

        return validationReportEntries;
    }

    private ValidationReportEntry createValidationReportEntry(IdVersion id) {
        String validationReportEntryMessage = getIdVersionLocation(id) + MESSAGE_FORMAT_UNRESOLVED_EXTERNAL_REFERENCE;
        return new ValidationReportEntry(validationReportEntryMessage, "NeTEx ID", ValidationReportEntrySeverity.ERROR, id.getFilename());
    }

    private String getIdVersionLocation(IdVersion id) {
        return "[Line " + id.getLineNumber() + ", Column " + id.getColumnNumber() + ", Id " + id.getId() + "] ";
    }

}
