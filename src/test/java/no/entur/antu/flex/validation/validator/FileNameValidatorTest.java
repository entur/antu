package no.entur.antu.flex.validation.validator;

import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class FileNameValidatorTest {

    private static final String TEST_CODESPACE = "TST";
    private static final String TEST_VALIDATION_REPORT_ID = "TEST_VALIDATION_REPORT_ID";
    private FileNameValidator fileNameValidator;
    private ValidationReport validationReport;

    @BeforeEach
    void setUpTest() {
        ValidationReportEntryFactory validationReportEntryFactory = (code, validationReportEntryMessage, dataLocation) ->
                new ValidationReportEntry(validationReportEntryMessage, code, ValidationReportEntrySeverity.INFO);
        fileNameValidator = new FileNameValidator(validationReportEntryFactory);
        validationReport = new ValidationReport(TEST_CODESPACE, TEST_VALIDATION_REPORT_ID);
    }

    @Test
    void nonXmlFileShouldBeIgnored() {
        String fileName = "_TST_flexible_shared_data.txt";
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, fileName, Set.of(), List.of());
        fileNameValidator.validate(validationReport, validationContext);

        Assertions.assertTrue(validationReport.getValidationReportEntries().isEmpty());
    }

    @Test
    void sharedFileShouldBeValidated() {
        String fileName = "_TST_flexible_shared_data.xml";
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, fileName, Set.of(), List.of());
        fileNameValidator.validate(validationReport, validationContext);

        Assertions.assertTrue(validationReport.getValidationReportEntries().isEmpty());
    }

    @Test
    void sharedFileShouldBeInvalidated() {
        String fileName = "_TST.xml";
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, fileName, Set.of(), List.of());
        fileNameValidator.validate(validationReport, validationContext);

        Assertions.assertFalse(validationReport.getValidationReportEntries().isEmpty());
        Assertions.assertTrue(validationReport.getValidationReportEntries().stream()
                .anyMatch(validationReportEntry -> validationReportEntry.getName().equals(FileNameValidator.RULE_CODE_NETEX_FILE_NAME_1))
        );
    }

    @Test
    void lineFileShouldBeValidated() {
        String fileName = "TST_34234234.xml";
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, fileName, Set.of(), List.of());
        fileNameValidator.validate(validationReport, validationContext);

        Assertions.assertTrue(validationReport.getValidationReportEntries().isEmpty());
    }

    @Test
    void lineFileShouldBeInvalidated() {
        String fileName = "TST.xml";
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, fileName, Set.of(), List.of());
        fileNameValidator.validate(validationReport, validationContext);

        Assertions.assertFalse(validationReport.getValidationReportEntries().isEmpty());
        Assertions.assertTrue(validationReport.getValidationReportEntries().stream()
                .anyMatch(validationReportEntry -> validationReportEntry.getName().equals(FileNameValidator.RULE_CODE_NETEX_FILE_NAME_1))
        );
    }
}