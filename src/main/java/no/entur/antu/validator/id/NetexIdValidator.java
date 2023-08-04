package no.entur.antu.validator.id;

import no.entur.antu.validator.NetexCodespace;
import org.entur.netex.validation.validator.AbstractNetexValidator;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validate that NeTEX IDs have a valid structure.
 */
public class NetexIdValidator extends AbstractNetexValidator {

    static final String RULE_CODE_NETEX_ID_2 = "NETEX_ID_2";
    static final String RULE_CODE_NETEX_ID_3 = "NETEX_ID_3";
    static final String RULE_CODE_NETEX_ID_4 = "NETEX_ID_4";
    static final String RULE_CODE_NETEX_ID_4W = "NETEX_ID_4W";

    private static final Logger LOGGER = LoggerFactory.getLogger(NetexIdValidator.class);

    private static final String REGEXP_VALID_ID = "^([A-Z]{3}):([A-Za-z]*):([0-9A-Za-z_\\-]*)$";
    private static final Pattern PATTERN_VALID_ID = Pattern.compile(REGEXP_VALID_ID);
    private static final String MESSAGE_FORMAT_INVALID_ID_STRUCTURE = "Invalid id structure on element";
    private static final String MESSAGE_FORMAT_INVALID_ID_NAME = "Invalid structure on id %s. Expected %s";
    private static final String MESSAGE_FORMAT_UNAPPROVED_CODESPACE = "Use of unapproved codespace. Approved codespaces are %s";

    private final Set<String> entityTypesReportedAsWarningForUnapprovedCodespace;

    public NetexIdValidator(ValidationReportEntryFactory validationReportEntryFactory) {
        this(validationReportEntryFactory, Set.of());
    }

    public NetexIdValidator(ValidationReportEntryFactory validationReportEntryFactory,
                            Set<String> entityTypesReportedAsWarningForUnapprovedCodespace) {
        super(validationReportEntryFactory);
        this.entityTypesReportedAsWarningForUnapprovedCodespace =
                Objects.requireNonNull(entityTypesReportedAsWarningForUnapprovedCodespace);
    }

    @Override
    public void validate(ValidationReport validationReport, ValidationContext validationContext) {
        validationReport.addAllValidationReportEntries(validate(validationContext));
    }

    protected List<ValidationReportEntry> validate(ValidationContext validationContext) {

        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        Set<String> validNetexCodespaces = NetexCodespace.getValidNetexCodespacesFor(validationContext.getCodespace())
                .stream()
                .map(NetexCodespace::xmlns)
                .collect(Collectors.toSet());
        String validNetexCodespaceList = String.join(",", validNetexCodespaces);

        for (IdVersion id : validationContext.getLocalIds()) {
            Matcher m = PATTERN_VALID_ID.matcher(id.getId());
            DataLocation dataLocation = getIdVersionLocation(id);
            if (!m.matches()) {
                validationReportEntries.add(createValidationReportEntry(RULE_CODE_NETEX_ID_2, dataLocation, MESSAGE_FORMAT_INVALID_ID_STRUCTURE));
                LOGGER.debug("Id {} has an invalid format. Valid format is {}", id, REGEXP_VALID_ID);
            } else {
                if (!m.group(2).equals(id.getElementName())) {
                    String expectedId = m.group(1) + ":" + id.getElementName() + ":" + m.group(3);
                    String validationReportEntryMessage = String.format(MESSAGE_FORMAT_INVALID_ID_NAME, id.getId(), expectedId);
                    validationReportEntries.add(createValidationReportEntry(RULE_CODE_NETEX_ID_3, dataLocation, validationReportEntryMessage));
                    LOGGER.debug("Id {} has an invalid format for the name part. Expected {}", id, expectedId);
                }

                String prefix = m.group(1);
                if (!validNetexCodespaces.contains(prefix)) {
                    String validationReportEntryMessage = String.format(MESSAGE_FORMAT_UNAPPROVED_CODESPACE, validNetexCodespaceList);
                    LOGGER.debug("Id {} uses an unapproved codespace prefix. Approved codespaces are: {}", id, validNetexCodespaceList);
                    if (entityTypesReportedAsWarningForUnapprovedCodespace.contains(id.getElementName())) {
                        validationReportEntries.add(createValidationReportEntry(RULE_CODE_NETEX_ID_4W, dataLocation, validationReportEntryMessage));
                    } else {
                        validationReportEntries.add(createValidationReportEntry(RULE_CODE_NETEX_ID_4, dataLocation, validationReportEntryMessage));
                    }
                }

            }
        }
        return validationReportEntries;
    }

    @Override
    public Set<String> getRuleDescriptions() {
        return Set.of(createRuleDescription(RULE_CODE_NETEX_ID_2, MESSAGE_FORMAT_INVALID_ID_STRUCTURE),
                createRuleDescription(RULE_CODE_NETEX_ID_3, MESSAGE_FORMAT_INVALID_ID_NAME),
                createRuleDescription((RULE_CODE_NETEX_ID_4), MESSAGE_FORMAT_UNAPPROVED_CODESPACE),
                createRuleDescription((RULE_CODE_NETEX_ID_4W), MESSAGE_FORMAT_UNAPPROVED_CODESPACE));
    }

}
