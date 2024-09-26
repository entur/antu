package no.entur.antu.validation.validator.servicelink.distance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
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
    String validationReportId = "Test1122";

    ValidationReport testValidationReport = new ValidationReport(
      TEST_CODESPACE,
      validationReportId
    );

    try (
      InputStream testDatasetAsStream = getClass()
        .getResourceAsStream('/' + testFile)
    ) {
      assert testDatasetAsStream != null;
      NetexEntitiesIndex netexEntitiesIndex = NETEX_PARSER.parse(
        testDatasetAsStream
      );

      JAXBValidationContext validationContext = mock(
        JAXBValidationContext.class
      );

      when(validationContext.isCommonFile()).thenReturn(false);
      when(validationContext.getValidationReportId())
        .thenReturn(validationReportId);
      when(validationContext.getNetexEntitiesIndex())
        .thenReturn(netexEntitiesIndex);
      when(validationContext.getNetexDataRepository()).thenReturn(null);
      when(validationContext.getStopPlaceRepository()).thenReturn(null);

      UnexpectedDistanceInServiceLinkValidator unexpectedDistanceInServiceLinkValidator =
        new UnexpectedDistanceInServiceLinkValidator(
            (code, message, dataLocation) ->
          new ValidationReportEntry(
            message,
            code,
            ValidationReportEntrySeverity.ERROR
          )
        );

      unexpectedDistanceInServiceLinkValidator.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
