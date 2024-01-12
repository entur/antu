package no.entur.antu.validator.id;

import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class NetexIdValidatorTest {

    private static final String TEST_CODESPACE = "TST";
    private static final String TEST_NETEX_ELEMENT_NAME = "NetexElementName";
    private static final String TEST_VALIDATION_REPORT_ID = "TEST_VALIDATION_REPORT_ID";
    private static final String TEST_ID_INVALID_STRUCTURE = "This is an invalid NeTEx ID";
    private static final String TEST_ID_INVALID_ENTITY_NAME = "XXX:YY:ZZ";

    private static final String TEST_ID_UNAPPROVED_CODESPACE = "XXX:NetexElementName:ZZ";

    private static final String TEST_ENTITY_TYPE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE = "EntityTypeReportedAsWarning";
    private static final String TEST_ID_UNAPPROVED_CODESPACE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE = "XXX:" + TEST_ENTITY_TYPE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE + ":ZZ";

    private static final String TEST_ID_VALID = TEST_CODESPACE + ":" + TEST_NETEX_ELEMENT_NAME + ":ZZZ";

    private ValidationReportEntryFactory validationReportEntryFactory;
    private NetexIdValidator netexIdValidator;
    private ValidationReport validationReport;

    @BeforeEach
    void setUpTest() {
        validationReportEntryFactory = (code, validationReportEntryMessage, dataLocation) -> new ValidationReportEntry(validationReportEntryMessage, code, ValidationReportEntrySeverity.INFO);
        netexIdValidator = new NetexIdValidator(validationReportEntryFactory);
        validationReport = new ValidationReport(TEST_CODESPACE, TEST_VALIDATION_REPORT_ID);
    }

    @Test
    void testInvalidIdStructure() {
        IdVersion idVersion1 = new IdVersion(TEST_ID_INVALID_STRUCTURE, null, TEST_NETEX_ELEMENT_NAME, null, null, 0, 0);
        Set<IdVersion> localIds = Set.of(idVersion1);
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, null, localIds, List.of());
        netexIdValidator.validate(validationReport, validationContext);
        Assertions.assertFalse(validationReport.getValidationReportEntries().isEmpty());
        Assertions.assertTrue(validationReport.getValidationReportEntries().stream().anyMatch(validationReportEntry -> validationReportEntry.getName().equals(NetexIdValidator.RULE_CODE_NETEX_ID_2)));
    }

    @Test
    void testInvalidIdEntityName() {
        IdVersion idVersion1 = new IdVersion(TEST_ID_INVALID_ENTITY_NAME, null, TEST_NETEX_ELEMENT_NAME, null, null, 0, 0);
        Set<IdVersion> localIds = Set.of(idVersion1);
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, null, localIds, List.of());
        netexIdValidator.validate(validationReport, validationContext);
        Assertions.assertFalse(validationReport.getValidationReportEntries().isEmpty());
        Assertions.assertTrue(validationReport.getValidationReportEntries().stream().anyMatch(validationReportEntry -> validationReportEntry.getName().equals(NetexIdValidator.RULE_CODE_NETEX_ID_3)));
    }

    @Test
    void testUnapprovedCodespace() {
        IdVersion idVersion1 = new IdVersion(TEST_ID_UNAPPROVED_CODESPACE, null, TEST_NETEX_ELEMENT_NAME, null, null, 0, 0);
        Set<IdVersion> localIds = Set.of(idVersion1);
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, null, localIds, List.of());
        netexIdValidator.validate(validationReport, validationContext);
        Assertions.assertFalse(validationReport.getValidationReportEntries().isEmpty());
        Assertions.assertTrue(validationReport.getValidationReportEntries().stream().anyMatch(validationReportEntry -> validationReportEntry.getName().equals(NetexIdValidator.RULE_CODE_NETEX_ID_4)));
    }

    @Test
    void testUnapprovedCodespaceWarning() {

        netexIdValidator = new NetexIdValidator(validationReportEntryFactory, Set.of(TEST_ENTITY_TYPE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE));

        IdVersion idVersion1 = new IdVersion(TEST_ID_UNAPPROVED_CODESPACE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE, null, TEST_ENTITY_TYPE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE, null, null, 0, 0);
        Set<IdVersion> localIds = Set.of(idVersion1);
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, null, localIds, List.of());
        netexIdValidator.validate(validationReport, validationContext);
        Assertions.assertFalse(validationReport.getValidationReportEntries().isEmpty());
        Assertions.assertTrue(validationReport.getValidationReportEntries().stream().anyMatch(validationReportEntry -> validationReportEntry.getName().equals(NetexIdValidator.RULE_CODE_NETEX_ID_4W)));
    }

    @Test
    void testValidId() {
        IdVersion idVersion1 = new IdVersion(TEST_ID_VALID, null, TEST_NETEX_ELEMENT_NAME, null, null, 0, 0);
        Set<IdVersion> localIds = Set.of(idVersion1);
        ValidationContext validationContext = new ValidationContext(null, null, TEST_CODESPACE, null, localIds, List.of());
        netexIdValidator.validate(validationReport, validationContext);
        Assertions.assertTrue(validationReport.getValidationReportEntries().isEmpty());
    }

}
