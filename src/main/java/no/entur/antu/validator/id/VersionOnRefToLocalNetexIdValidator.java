package no.entur.antu.validator.id;

import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validate that references to local elements have a version attribute.
 */
public class VersionOnRefToLocalNetexIdValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionOnRefToLocalNetexIdValidator.class);


    private static final String MESSAGE_FORMAT_MISSING_VERSION_ON_REF_TO_LOCAL_ID = "Missing version attribute on reference to local elements";

    public List<ValidationReportEntry> validate(Set<IdVersion> localIds, List<IdVersion> localRefs) {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        List<IdVersion> nonVersionedLocalRefs = localRefs.stream().filter(e -> e.getVersion() == null).collect(Collectors.toList());
        Set<String> localIdsWithoutVersion = localIds.stream().map(IdVersion::getId).collect(Collectors.toSet());
        for (IdVersion id : nonVersionedLocalRefs) {
            if (localIdsWithoutVersion.contains(id.getId())) {
                String validationReportEntryMessage = getIdVersionLocation(id) + MESSAGE_FORMAT_MISSING_VERSION_ON_REF_TO_LOCAL_ID;
                validationReportEntries.add(new ValidationReportEntry(validationReportEntryMessage, "NeTEx ID", ValidationReportEntrySeverity.ERROR, id.getFilename()));
                LOGGER.debug("Found local reference to {} in line file without use of version-attribute", id.getId());
            }

        }

        return validationReportEntries;

    }

    private String getIdVersionLocation(IdVersion id) {
        return "[Line " + id.getLineNumber() + ", Column " + id.getColumnNumber() + ", Id " + id.getId() + "] ";
    }


}
