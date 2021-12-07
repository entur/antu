package no.entur.antu.validator.id;

import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.validator.codespace.NetexCodespace;
import no.entur.antu.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validate that NeTEX IDs have a valid structure.
 */
public class NetexIdValidator {

    public static final String REGEXP_VALID_ID = "^([A-Z]{3}):([A-Za-z]*):([0-9A-Za-z_\\-]*)$";
    public static final Pattern PATTERN_VALID_ID = Pattern.compile(REGEXP_VALID_ID);

    private static final Logger LOGGER = LoggerFactory.getLogger(NetexIdValidator.class);
    public static final String MESSAGE_FORMAT_INVALID_ID_STRUCTURE = "Invalid id structure on element";
    public static final String MESSAGE_FORMAT_INVALID_ID_NAME = "Invalid structure on id %s. Expected %s";
    public static final String MESSAGE_FORMAT_UNAPPROVED_CODESPACE = "Use of unapproved codespace. Approved codespaces are %s";

    public List<ValidationReportEntry> validateIdStructure(ValidationContext validationContext, Set<IdVersion> localIds) {

        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        Set<String> validNetexCodespaces = NetexCodespace.getValidNetexCodespacesFor(validationContext.getCodespace()).stream().map(NetexCodespace::getXmlns).collect(Collectors.toSet());
        String validNetexCodespaceList = String.join(",", validNetexCodespaces);

        for (IdVersion id : localIds) {
            Matcher m = PATTERN_VALID_ID.matcher(id.getId());
            if (!m.matches()) {
                String validationReportEntryMessage = idVersionLocation(id) + MESSAGE_FORMAT_INVALID_ID_STRUCTURE;
                validationReportEntries.add(new ValidationReportEntry(validationReportEntryMessage, "NeTEx ID", ValidationReportEntrySeverity.ERROR));
                LOGGER.debug("Id {} in line file have an invalid format. Correct format is {}", id, REGEXP_VALID_ID);
            } else {
                if (!m.group(2).equals(id.getElementName())) {
                    String expectedId = m.group(1) + ":" + id.getElementName() + ":" + m.group(3);
                    String validationReportEntryMessage = idVersionLocation(id) + String.format(MESSAGE_FORMAT_INVALID_ID_NAME, id.getId(), expectedId);
                    validationReportEntries.add(new ValidationReportEntry(validationReportEntryMessage, "NeTEx ID", ValidationReportEntrySeverity.ERROR));
                    LOGGER.debug("Id {} in file have an invalid format for the name part. Expected {}", id, expectedId);
                }

                String prefix = m.group(1);
                if (!validNetexCodespaces.contains(prefix)) {

                    String validationReportEntryMessage = idVersionLocation(id) + String.format(MESSAGE_FORMAT_UNAPPROVED_CODESPACE, validNetexCodespaceList);
                    validationReportEntries.add(new ValidationReportEntry(validationReportEntryMessage, "NeTEx ID", ValidationReportEntrySeverity.ERROR));
                    LOGGER.debug("Id {} in file have an invalid format for the name part. Expected {}", id, validNetexCodespaceList);
                }

            }
        }

        return validationReportEntries;

    }

    private String idVersionLocation(IdVersion id) {
        return "[Line " + id.getLineNumber() + ", Column " + id.getColumnNumber() + ", Id " + id.getId() + "] ";
    }


}
