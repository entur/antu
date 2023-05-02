package no.entur.antu.validator;

import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Modify the content of a validation report.
 */
public class ValidationReportTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationReportTransformer.class);

    private final int maxValidationReportEntriesPerRule;

    /**
     * @param maxValidationReportEntriesPerRule the maximum number of entries per rule. Additional entries are removed from the report.
     */
    public ValidationReportTransformer(int maxValidationReportEntriesPerRule) {
        this.maxValidationReportEntriesPerRule = maxValidationReportEntriesPerRule;
    }

    /**
     * Limit the number of validation entries per rule.
     *
     * @param validationReport the report to truncate.
     */
    public void truncate(ValidationReport validationReport) {
        Collection<ValidationReportEntry> validationReportEntries = validationReport.getValidationReportEntries();
        int nbEntries = validationReportEntries.size();
        Map<String, List<ValidationReportEntry>> validationReportEntriesByRuleName = validationReportEntries.stream().collect(Collectors.groupingBy(ValidationReportEntry::getName));
        validationReportEntriesByRuleName.replaceAll((ruleName, reportEntries) -> reportEntries.stream().limit(maxValidationReportEntriesPerRule).toList());
        List<ValidationReportEntry> truncatedValidationReportEntries = validationReportEntriesByRuleName.values().stream().flatMap(Collection::stream).toList();
        if (truncatedValidationReportEntries.size() < nbEntries) {
            validationReportEntries.clear();
            validationReportEntries.addAll(truncatedValidationReportEntries);
            LOGGER.info("Truncated {} entries in the validation report {}", nbEntries - validationReportEntries.size(), validationReport.getValidationReportId());
        }
    }

}
