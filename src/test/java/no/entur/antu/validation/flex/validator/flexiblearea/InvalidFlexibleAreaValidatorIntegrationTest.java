package no.entur.antu.validation.flex.validator.flexiblearea;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.junit.jupiter.api.Test;

class InvalidFlexibleAreaValidatorIntegrationTest {

  public static final String _ATB_FLEXIBLE_SHARED_DATA =
    "_ATB_flexible_shared_data.xml";

  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testSelfInteractingRingShouldBeReported() throws IOException {
    ValidationReport validationReport = getValidationReport(
      _ATB_FLEXIBLE_SHARED_DATA,
      "ATB"
    );
    assertEquals(2, validationReport.getValidationReportEntries().size());
  }

  private ValidationReport getValidationReport(
    String testFile,
    String codeSpace
  ) throws IOException {
    String validationReportId = "Test1122";
    ValidationReport testValidationReport = new ValidationReport(
      codeSpace,
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

      when(validationContext.isCommonFile()).thenReturn(true);
      when(validationContext.getValidationReportId())
        .thenReturn(validationReportId);
      when(validationContext.getNetexEntitiesIndex())
        .thenReturn(netexEntitiesIndex);
      when(validationContext.getNetexDataRepository())
        .thenReturn(mock(NetexDataRepository.class));
      when(validationContext.getStopPlaceRepository())
        .thenReturn(mock(StopPlaceRepository.class));

      InvalidFlexibleAreaValidator invalidFlexibleAreaValidator =
        new InvalidFlexibleAreaValidator((code, message, dataLocation) ->
          new ValidationReportEntry(
            message,
            code,
            ValidationReportEntrySeverity.ERROR
          )
        );

      invalidFlexibleAreaValidator.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
