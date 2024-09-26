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
import no.entur.antu.validation.validator.passengerstopassignment.MissingPassengerStopAssignmentValidator;
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

class MissingPassengerStopAssignmentValidatorIntegrationTest {

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
    NetexDataRepository netexDataRepository = Mockito.mock(
      NetexDataRepository.class
    );

    // Mocking that the quay ids are not present in the common data repository.
    // Validator will try to fetch quay ids from the line file instead.
    Mockito.when(netexDataRepository.hasQuayIds(anyString())).thenReturn(false);

    ValidationReport validationReport = getValidationReport(
      TEST_FILE_WITH_NO_COMPOSITE_FRAME,
      netexDataRepository
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
    NetexDataRepository netexDataRepository = Mockito.mock(
      NetexDataRepository.class
    );

    // Mocking that the quay ids are not present in the common data repository.
    // Validator will try to fetch quay ids from the line file instead.
    Mockito.when(netexDataRepository.hasQuayIds(anyString())).thenReturn(false);

    ValidationReport validationReport = getValidationReport(
      TEST_FILE_WITH_NO_COMPOSITE_FRAME_DEAD_RUN,
      netexDataRepository
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  private ValidationReport getValidationReport(
    String testFile,
    NetexDataRepository netexDataRepository
  ) throws IOException {
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

      StopPlaceRepository stopPlaceRepository = Mockito.mock(
        StopPlaceRepository.class
      );

      when(stopPlaceRepository.getStopPlaceNameForQuayId(any()))
        .thenReturn("TestName");

      when(validationContext.isCommonFile()).thenReturn(false);
      when(validationContext.getValidationReportId())
        .thenReturn(validationReportId);
      when(validationContext.getNetexEntitiesIndex())
        .thenReturn(netexEntitiesIndex);
      when(validationContext.getNetexDataRepository())
        .thenReturn(netexDataRepository);
      when(validationContext.getStopPlaceRepository())
        .thenReturn(stopPlaceRepository);

      MissingPassengerStopAssignmentValidator missingPassengerStopAssignmentValidator =
        new MissingPassengerStopAssignmentValidator(
            (code, message, dataLocation) ->
          new ValidationReportEntry(
            message,
            code,
            ValidationReportEntrySeverity.ERROR
          )
        );

      missingPassengerStopAssignmentValidator.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
