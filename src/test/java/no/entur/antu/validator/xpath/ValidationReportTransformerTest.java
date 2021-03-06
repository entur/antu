package no.entur.antu.validator.xpath;

import no.entur.antu.validator.ValidationReportTransformer;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

class ValidationReportTransformerTest {

    public static final String RULE_1 = "RULE1";
    public static final String RULE_2 = "RULE2";
    public static final String RULE_3 = "RULE3";

    @Test
    void testTruncate() {
        Collection<ValidationReportEntry> entries = List.of(
                new ValidationReportEntry("", RULE_1, ValidationReportEntrySeverity.INFO),
                new ValidationReportEntry("", RULE_2, ValidationReportEntrySeverity.INFO),
                new ValidationReportEntry("", RULE_2, ValidationReportEntrySeverity.INFO),
                new ValidationReportEntry("", RULE_3, ValidationReportEntrySeverity.INFO),
                new ValidationReportEntry("", RULE_3, ValidationReportEntrySeverity.INFO),
                new ValidationReportEntry("", RULE_3, ValidationReportEntrySeverity.INFO)
        );
        ValidationReport validationReport = new ValidationReport("codespace", "report-id", entries);
        ValidationReportTransformer validationReportTransformer = new ValidationReportTransformer(2);
        validationReportTransformer.truncate(validationReport);
        Assertions.assertEquals(1, validationReport.getValidationReportEntries().stream().filter(validationReportEntry -> RULE_1.equals(validationReportEntry.getName())).count());
        Assertions.assertEquals(2, validationReport.getValidationReportEntries().stream().filter(validationReportEntry -> RULE_2.equals(validationReportEntry.getName())).count());
        Assertions.assertEquals(2, validationReport.getValidationReportEntries().stream().filter(validationReportEntry -> RULE_3.equals(validationReportEntry.getName())).count());

    }
}
