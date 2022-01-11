package no.entur.antu.validator.id;

import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validate that references refer to an existing element.
 */
public class NeTexReferenceValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NeTexReferenceValidator.class);

    private static final String MESSAGE_FORMAT_UNRESOLVED_EXTERNAL_REFERENCE = "Unresolved reference to external reference data";
    private final List<ExternalReferenceValidator> externalReferenceValidators;
    private final CommonNetexIdRepository commonNetexIdRepository;

    public NeTexReferenceValidator(CommonNetexIdRepository commonNetexIdRepository, List<ExternalReferenceValidator> externalReferenceValidators) {
        this.commonNetexIdRepository = commonNetexIdRepository;
        this.externalReferenceValidators = externalReferenceValidators;
    }

    public List<ValidationReportEntry> validate(String reportId, List<IdVersion> netexRefs, List<IdVersion> localIds) {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        // Remove duplicates, that is: references that have the same id and version (see #IdVersion.equals)
        Set<IdVersion> possibleExternalReferences = new HashSet<>(netexRefs);
        // Remove references that are found in local ids, comparing by id and version
        possibleExternalReferences.removeAll(localIds);
        if (!possibleExternalReferences.isEmpty()) {
            // Remove references that are found in the common files, comparing only by id, not by id and version
            Set<String> commonIds = commonNetexIdRepository.getCommonNetexIds(reportId);
            possibleExternalReferences.removeIf(ref -> commonIds.stream().anyMatch(commonId -> commonId.equals(ref.getId())));
            if (!possibleExternalReferences.isEmpty()) {
                // Remove references that are valid according to the external id validators
                externalReferenceValidators.forEach(validator -> possibleExternalReferences.removeAll(validator.validateReferenceIds(possibleExternalReferences)));
                if (!possibleExternalReferences.isEmpty()) {
                    for (IdVersion id : possibleExternalReferences) {
                        LOGGER.debug("Unable to validate external reference {}", id);
                        validationReportEntries.add(createValidationReportEntry(id));
                    }
                }
            }
        }


        return validationReportEntries;
    }

    private ValidationReportEntry createValidationReportEntry(IdVersion id) {
        String validationReportEntryMessage = getIdVersionLocation(id) + MESSAGE_FORMAT_UNRESOLVED_EXTERNAL_REFERENCE;
        return new ValidationReportEntry(validationReportEntryMessage, "NETEX_ID_5", ValidationReportEntrySeverity.ERROR, id.getFilename());
    }


    private String getIdVersionLocation(IdVersion id) {
        return "[Line " + id.getLineNumber() + ", Column " + id.getColumnNumber() + ", Id " + id.getId() + "] ";
    }

}
