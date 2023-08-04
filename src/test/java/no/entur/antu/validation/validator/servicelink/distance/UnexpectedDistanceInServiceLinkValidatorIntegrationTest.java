package no.entur.antu.validation.validator.servicelink.distance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;

class UnexpectedDistanceInServiceLinkValidatorIntegrationTest {

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

      AntuNetexData antuNetexData = mock(AntuNetexData.class);
      when(antuNetexData.netexEntitiesIndex()).thenReturn(netexEntitiesIndex);
      when(validationContext.isCommonFile()).thenReturn(false);
      when(validationContext.getAntuNetexData(anyString(), any(), any()))
        .thenReturn(antuNetexData);

      UnexpectedDistanceInServiceLinkValidator unexpectedDistanceInServiceLinkValidator =
        new UnexpectedDistanceInServiceLinkValidator(
          (code, message, dataLocation) ->
            new ValidationReportEntry(
              message,
              code,
              ValidationReportEntrySeverity.ERROR
            ),
          null,
          null
        );

      unexpectedDistanceInServiceLinkValidator.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
