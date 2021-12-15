package no.entur.antu.validator.id;

import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Verify that NeTEx ids in the current file are not already present in one of the previous files.
 *
 */
public class NetexIdUniquenessValidator {

    private static final String MESSAGE_FORMAT_DUPLICATE_ID_ACROSS_FILES = "Duplicate element identifiers across files";

    public List<ValidationReportEntry> validate(Set<String> accumulatedLocalIds, Map<String, IdVersion> netexFileLocalIds, String fileName) {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
        Set<String> duplicateIds = new HashSet<>(netexFileLocalIds.keySet());
        duplicateIds.retainAll(accumulatedLocalIds);
        if (!duplicateIds.isEmpty()) {
            for (String id : duplicateIds) {
                String validationReportEntryMessage = getIdVersionLocation(netexFileLocalIds.get(id)) + MESSAGE_FORMAT_DUPLICATE_ID_ACROSS_FILES;
                validationReportEntries.add(new ValidationReportEntry(validationReportEntryMessage, "NeTEx ID Consistency", ValidationReportEntrySeverity.ERROR, fileName));
            }
        }
        accumulatedLocalIds.addAll(netexFileLocalIds.keySet());
        return validationReportEntries;
    }

    private String getIdVersionLocation(IdVersion id) {
        return "[Line " + id.getLineNumber() + ", Column " + id.getColumnNumber() + ", Id " + id.getId() + "] ";
    }
}
