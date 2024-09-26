package no.entur.antu.validation.validator.servicejourney.speed;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.Mockito;

class UnexpectedSpeedValidatorIntegrationTest {

  public static final String TEST_CODESPACE = "ENT";
  public static final String TEST_FILE_WITH_NO_COMPOSITE_FRAME =
    "ENT_No_Composite_Frame.xml";
  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testNoPassengerStopAssignmentsShouldIgnoreValidationGracefully()
    throws IOException {
    ValidationReport validationReport = getValidationReport(
      TEST_FILE_WITH_NO_COMPOSITE_FRAME
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

      NetexDataRepository commonDataRepository = Mockito.mock(
        NetexDataRepository.class
      );

      Mockito
        .when(commonDataRepository.hasQuayIds(anyString()))
        .thenReturn(false);

      when(validationContext.isCommonFile()).thenReturn(false);
      when(validationContext.getValidationReportId())
        .thenReturn(validationReportId);
      when(validationContext.getNetexEntitiesIndex())
        .thenReturn(netexEntitiesIndex);
      when(validationContext.getNetexDataRepository())
        .thenReturn(commonDataRepository);
      when(validationContext.getStopPlaceRepository())
        .thenReturn(mock(StopPlaceRepository.class));

      UnexpectedSpeedValidator unexpectedSpeedValidator =
        new UnexpectedSpeedValidator((code, message, dataLocation) ->
          new ValidationReportEntry(
            message,
            code,
            ValidationReportEntrySeverity.ERROR
          )
        );

      unexpectedSpeedValidator.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
