package no.entur.antu.validation.validator.servicejourney.passingtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.junit.jupiter.api.Test;

class NonIncreasingTimetabledPassingTimeValidatorIntegrationTest {

  public static final String TEST_CODESPACE = "FLB";
  public static final String TEST_FILE_VALID =
    "FLB_FLB-Line-42_42_Flamsbana.xml";
  public static final String TEST_FILE_VALID_INVALID =
    "FLB_FLB-Line-42_42_Flamsbana_non_increasing_passing_time.xml";
  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testValidIncreasingPassingTime() throws IOException {
    ValidationReport validationReport = getValidationReport(TEST_FILE_VALID);
    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testInValidIncreasingPassingTime() throws IOException {
    ValidationReport validationReport = getValidationReport(
      TEST_FILE_VALID_INVALID
    );
    assertEquals(3, validationReport.getValidationReportEntries().size());
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

      NetexDataRepository netexDataRepository = mock(NetexDataRepository.class);
      when(netexDataRepository.hasQuayIds(anyString())).thenReturn(true);

      when(validationContext.getValidationReportId())
        .thenReturn(validationReportId);
      when(validationContext.getNetexEntitiesIndex())
        .thenReturn(netexEntitiesIndex);
      when(validationContext.getNetexDataRepository())
        .thenReturn(netexDataRepository);
      when(validationContext.getStopPlaceRepository())
        .thenReturn(mock(StopPlaceRepository.class));

      NonIncreasingPassingTimeValidator nonIncreasingPassingTimeValidator =
        new NonIncreasingPassingTimeValidator((code, message, dataLocation) ->
          new ValidationReportEntry(
            message,
            code,
            ValidationReportEntrySeverity.ERROR
          )
        );

      nonIncreasingPassingTimeValidator.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
