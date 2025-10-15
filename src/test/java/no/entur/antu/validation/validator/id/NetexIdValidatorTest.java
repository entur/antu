package no.entur.antu.validation.validator.id;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetexIdValidatorTest {

  private static final String TEST_CODESPACE = "TST";
  private static final String TEST_NETEX_ELEMENT_NAME = "NetexElementName";
  private static final String TEST_VALIDATION_REPORT_ID =
    "TEST_VALIDATION_REPORT_ID";
  private static final String TEST_ID_INVALID_STRUCTURE =
    "This is an invalid NeTEx ID";
  private static final String TEST_ID_INVALID_ENTITY_NAME = "XXX:YY:ZZ";

  private static final String TEST_ID_UNAPPROVED_CODESPACE =
    "XXX:NetexElementName:ZZ";

  private static final String TEST_ENTITY_TYPE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE =
    "EntityTypeReportedAsWarning";
  private static final String TEST_ID_UNAPPROVED_CODESPACE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE =
    "XXX:" +
    TEST_ENTITY_TYPE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE +
    ":ZZ";

  private static final String TEST_ID_VALID =
    TEST_CODESPACE + ":" + TEST_NETEX_ELEMENT_NAME + ":ZZZ";

  private NetexIdValidator netexIdValidator;

  @BeforeEach
  void setUpTest() {
    netexIdValidator = new NetexIdValidator(new HashSet<>());
  }

  @Test
  void testInvalidIdStructure() {
    IdVersion idVersion1 = new IdVersion(
      TEST_ID_INVALID_STRUCTURE,
      null,
      TEST_NETEX_ELEMENT_NAME,
      null,
      null,
      0,
      0
    );
    Set<IdVersion> localIds = Set.of(idVersion1);
    XPathValidationContext validationContext = createValidationContext(
      localIds
    );
    List<ValidationIssue> validationIssues = netexIdValidator.validate(
      validationContext
    );
    Assertions.assertFalse(validationIssues.isEmpty());
    Assertions.assertTrue(
      validationIssues
        .stream()
        .anyMatch(validationReportEntry ->
          validationReportEntry
            .rule()
            .code()
            .equals(NetexIdValidator.RULE_INVALID_ID_STRUCTURE.code())
        )
    );
  }

  @Test
  void testInvalidIdEntityName() {
    IdVersion idVersion1 = new IdVersion(
      TEST_ID_INVALID_ENTITY_NAME,
      null,
      TEST_NETEX_ELEMENT_NAME,
      null,
      null,
      0,
      0
    );
    Set<IdVersion> localIds = Set.of(idVersion1);
    XPathValidationContext validationContext = createValidationContext(
      localIds
    );
    List<ValidationIssue> validationIssues = netexIdValidator.validate(
      validationContext
    );
    Assertions.assertFalse(validationIssues.isEmpty());
    Assertions.assertTrue(
      validationIssues
        .stream()
        .anyMatch(validationReportEntry ->
          validationReportEntry
            .rule()
            .code()
            .equals(NetexIdValidator.RULE_INVALID_ID_NAME.code())
        )
    );
  }

  @Test
  void testUnapprovedCodespace() {
    IdVersion idVersion1 = new IdVersion(
      TEST_ID_UNAPPROVED_CODESPACE,
      null,
      TEST_NETEX_ELEMENT_NAME,
      null,
      null,
      0,
      0
    );
    Set<IdVersion> localIds = Set.of(idVersion1);
    XPathValidationContext validationContext = createValidationContext(
      localIds
    );
    List<ValidationIssue> validationIssues = netexIdValidator.validate(
      validationContext
    );
    Assertions.assertFalse(validationIssues.isEmpty());
    Assertions.assertTrue(
      validationIssues
        .stream()
        .anyMatch(validationIssue ->
          validationIssue
            .rule()
            .code()
            .equals(NetexIdValidator.RULE_UNAPPROVED_CODESPACE.code())
        )
    );
  }

  @Test
  void testUnapprovedCodespaceWarning() {
    netexIdValidator =
      new NetexIdValidator(
        Set.of(TEST_ENTITY_TYPE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE),
        new HashSet<>()
      );

    IdVersion idVersion1 = new IdVersion(
      TEST_ID_UNAPPROVED_CODESPACE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE,
      null,
      TEST_ENTITY_TYPE_REPORTED_AS_WARNING_FOR_UNAPPROVED_CODESPACE,
      null,
      null,
      0,
      0
    );
    Set<IdVersion> localIds = Set.of(idVersion1);
    XPathValidationContext validationContext = createValidationContext(
      localIds
    );
    List<ValidationIssue> validationIssues = netexIdValidator.validate(
      validationContext
    );
    Assertions.assertFalse(validationIssues.isEmpty());
    Assertions.assertTrue(
      validationIssues
        .stream()
        .anyMatch(validationIssue ->
          validationIssue
            .rule()
            .code()
            .equals(NetexIdValidator.RULE_UNAPPROVED_CODESPACE_WARNING.code())
        )
    );
  }

  @Test
  void testValidId() {
    IdVersion idVersion1 = new IdVersion(
      TEST_ID_VALID,
      null,
      TEST_NETEX_ELEMENT_NAME,
      null,
      null,
      0,
      0
    );
    Set<IdVersion> localIds = Set.of(idVersion1);
    XPathValidationContext validationContext = createValidationContext(
      localIds
    );
    List<ValidationIssue> validationIssues = netexIdValidator.validate(
      validationContext
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  private static XPathValidationContext createValidationContext(
    Set<IdVersion> localIds
  ) {
    return new XPathValidationContext(
      null,
      null,
      TEST_CODESPACE,
      null,
      localIds,
      List.of(),
      TEST_VALIDATION_REPORT_ID
    );
  }
}
