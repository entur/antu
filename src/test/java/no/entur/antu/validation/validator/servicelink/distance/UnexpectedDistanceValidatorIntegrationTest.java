package no.entur.antu.validation.validator.servicelink.distance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;

class UnexpectedDistanceValidatorIntegrationTest {

  public static final String TEST_CODESPACE = "AVI";
  public static final String TEST_FILE_WITH_NO_SERVICE_LINKS =
    "_avinor_common_elements.xml";
  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testValidIncreasingPassingTime() throws IOException {
    ValidationReport validationReport = getValidationReport(
      TEST_FILE_WITH_NO_SERVICE_LINKS
    );
    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  private ValidationReport getValidationReport(String testFile)
    throws IOException {
    ValidationReport testValidationReport = new ValidationReport(
      TEST_CODESPACE,
      "Test1122"
    );

    try (
      InputStream testDatasetAsStream = getClass()
        .getResourceAsStream('/' + testFile)
    ) {
      assert testDatasetAsStream != null;
      NetexEntitiesIndex netexEntitiesIndex = NETEX_PARSER.parse(
        testDatasetAsStream
      );

      ValidationContextWithNetexEntitiesIndex validationContext = mock(
        ValidationContextWithNetexEntitiesIndex.class
      );
      when(validationContext.getNetexEntitiesIndex())
        .thenReturn(netexEntitiesIndex);
      when(validationContext.isCommonFile()).thenReturn(true);

      UnexpectedDistanceValidator unexpectedDistanceValidator =
        new UnexpectedDistanceValidator(
          (code, message, dataLocation) ->
            new ValidationReportEntry(
              message,
              code,
              ValidationReportEntrySeverity.ERROR
            ),
          null,
          null
        );

      unexpectedDistanceValidator.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
