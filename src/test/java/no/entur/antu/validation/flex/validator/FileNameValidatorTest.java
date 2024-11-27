package no.entur.antu.validation.flex.validator;

import java.util.List;
import java.util.Set;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileNameValidatorTest {

  private static final String TEST_CODESPACE = "TST";
  private static final String TEST_VALIDATION_REPORT_ID =
    "TEST_VALIDATION_REPORT_ID";
  private FileNameValidator fileNameValidator;

  @BeforeEach
  void setUpTest() {
    fileNameValidator = new FileNameValidator();
  }

  @Test
  void nonXmlFileShouldBeIgnored() {
    String fileName = "_TST_flexible_shared_data.txt";
    XPathValidationContext validationContext = createValidationContext(
      fileName
    );
    List<ValidationIssue> validationIssues = fileNameValidator.validate(
      validationContext
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void sharedFileShouldBeValidated() {
    String fileName = "_TST_flexible_shared_data.xml";
    XPathValidationContext validationContext = createValidationContext(
      fileName
    );
    List<ValidationIssue> validationIssues = fileNameValidator.validate(
      validationContext
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void sharedFileShouldBeInvalidated() {
    String fileName = "_TST.xml";
    XPathValidationContext validationContext = createValidationContext(
      fileName
    );
    List<ValidationIssue> validationIssues = fileNameValidator.validate(
      validationContext
    );
    Assertions.assertFalse(validationIssues.isEmpty());
    Assertions.assertTrue(
      validationIssues
        .stream()
        .anyMatch(validationIssue ->
          validationIssue.rule().code().equals(FileNameValidator.RULE.code())
        )
    );
  }

  @Test
  void lineFileShouldBeValidated() {
    String fileName = "TST_34234234.xml";
    XPathValidationContext validationContext = createValidationContext(
      fileName
    );
    List<ValidationIssue> validationIssues = fileNameValidator.validate(
      validationContext
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void lineFileShouldBeInvalidated() {
    String fileName = "TST.xml";
    XPathValidationContext validationContext = createValidationContext(
      fileName
    );
    List<ValidationIssue> validationIssues = fileNameValidator.validate(
      validationContext
    );
    Assertions.assertFalse(validationIssues.isEmpty());
    Assertions.assertTrue(
      validationIssues
        .stream()
        .anyMatch(validationIssue ->
          validationIssue.rule().code().equals(FileNameValidator.RULE.code())
        )
    );
  }

  private static XPathValidationContext createValidationContext(
    String fileName
  ) {
    return new XPathValidationContext(
      null,
      null,
      TEST_CODESPACE,
      fileName,
      Set.of(),
      List.of(),
      TEST_VALIDATION_REPORT_ID
    );
  }
}
