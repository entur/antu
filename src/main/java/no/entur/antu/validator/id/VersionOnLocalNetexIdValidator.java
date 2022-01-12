package no.entur.antu.validator.id;

import no.entur.antu.validator.NetexValidator;
import no.entur.antu.validator.ValidationReport;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validate that local NeTEX IDs have a version attribute.
 */
public class VersionOnLocalNetexIdValidator implements NetexValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionOnLocalNetexIdValidator.class);

    private static final String MESSAGE_FORMAT_MISSING_VERSION = "Missing version attribute on elements with id attribute";

    @Override
    public void validate(ValidationReport validationReport, ValidationContext validationContext) {
        validationReport.addAllValidationReportEntries(validate(validationContext.getLocalIds()));
    }

    protected List<ValidationReportEntry> validate(Set<IdVersion> localIds) {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
        Set<IdVersion> nonVersionedLocalIds = localIds.stream().filter(e -> e.getVersion() == null).collect(Collectors.toSet());
        if (!nonVersionedLocalIds.isEmpty()) {
            for (IdVersion id : nonVersionedLocalIds) {
                String validationReportEntryMessage = getIdVersionLocation(id) + MESSAGE_FORMAT_MISSING_VERSION;
                validationReportEntries.add(new ValidationReportEntry(validationReportEntryMessage, "NETEX_ID_8", ValidationReportEntrySeverity.ERROR, id.getFilename()));
                LOGGER.debug("Id {} does not have version attribute set", id.getId());
            }
        }
        return validationReportEntries;

    }

    private String getIdVersionLocation(IdVersion id) {
        return "[Line " + id.getLineNumber() + ", Column " + id.getColumnNumber() + ", Id " + id.getId() + "] ";
    }



}
