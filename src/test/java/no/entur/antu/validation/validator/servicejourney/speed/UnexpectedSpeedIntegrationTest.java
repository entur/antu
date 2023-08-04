package no.entur.antu.validation.validator.servicejourney.speed;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UnexpectedSpeedIntegrationTest {

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
      when(validationContext.isCommonFile()).thenReturn(false);

      CommonDataRepository commonDataRepository = Mockito.mock(
        CommonDataRepository.class
      );
      StopPlaceRepository stopPlaceRepository = Mockito.mock(
        StopPlaceRepository.class
      );

      Mockito
        .when(commonDataRepository.hasQuayIds(anyString()))
        .thenReturn(false);

      UnexpectedSpeed unexpectedSpeed = new UnexpectedSpeed(
        (code, message, dataLocation) ->
          new ValidationReportEntry(
            message,
            code,
            ValidationReportEntrySeverity.ERROR
          ),
        commonDataRepository,
        stopPlaceRepository
      );

      unexpectedSpeed.validate(testValidationReport, validationContext);
    }

    return testValidationReport;
  }
}
