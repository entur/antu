package no.entur.antu.validation.validator.passengerstopassignemnt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validation.validator.passengerstopassignment.MissingPassengerStopAssignment;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MissingPassengerStopAssignmentIntegrationTest {

  public static final String TEST_CODESPACE = "ENT";
  public static final String TEST_FILE_WITH_NO_COMPOSITE_FRAME =
    "ENT_No_Composite_Frame.xml";
  public static final String TEST_FILE_WITH_NO_COMPOSITE_FRAME_DEAD_RUN =
    "ENT_No_Composite_Frame_Dead_Run.xml";
  private static final NetexParser NETEX_PARSER = new NetexParser();

  /**
   * Since the dataset does not contain any passenger stop assignments,
   * the validator should try to validate and fail, because there are no Dead runs.
   */
  @Test
  void testNoPassengerStopAssignmentsInDatasetAndNoDeadRunShouldFail()
    throws IOException {
    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    // Mocking that the quay ids are not present in the common data repository.
    // Validator will try to fetch quay ids from the line file instead.
    Mockito
      .when(commonDataRepository.hasQuayIds(anyString()))
      .thenReturn(false);

    ValidationReport validationReport = getValidationReport(
      TEST_FILE_WITH_NO_COMPOSITE_FRAME,
      commonDataRepository
    );

    assertFalse(validationReport.getValidationReportEntries().isEmpty());
    assertEquals(18, validationReport.getValidationReportEntries().size());
  }

  /**
   * Since the dataset does not contain any passenger stop assignments,
   * the validator should try to validate and fail, because there are no Dead runs.
   */
  @Test
  void testNoPassengerStopAssignmentsInDatasetAndWithAllDeadRun()
    throws IOException {
    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    // Mocking that the quay ids are not present in the common data repository.
    // Validator will try to fetch quay ids from the line file instead.
    Mockito
      .when(commonDataRepository.hasQuayIds(anyString()))
      .thenReturn(false);

    ValidationReport validationReport = getValidationReport(
      TEST_FILE_WITH_NO_COMPOSITE_FRAME_DEAD_RUN,
      commonDataRepository
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  private ValidationReport getValidationReport(
    String testFile,
    CommonDataRepository commonDataRepository
  ) throws IOException {
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

      StopPlaceRepository stopPlaceRepository = Mockito.mock(
        StopPlaceRepository.class
      );

      when(stopPlaceRepository.getStopPlaceNameForQuayId(any()))
        .thenReturn("TestName");

      MissingPassengerStopAssignment missingPassengerStopAssignment =
        new MissingPassengerStopAssignment(
          (code, message, dataLocation) ->
            new ValidationReportEntry(
              message,
              code,
              ValidationReportEntrySeverity.ERROR
            ),
          commonDataRepository,
          stopPlaceRepository
        );

      missingPassengerStopAssignment.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
