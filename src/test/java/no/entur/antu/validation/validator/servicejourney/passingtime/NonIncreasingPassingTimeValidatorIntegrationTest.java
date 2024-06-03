package no.entur.antu.validation.validator.servicejourney.passingtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;

class NonIncreasingPassingTimeValidatorIntegrationTest {

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
    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    assertFalse(validationReportEntries.isEmpty());
    assertEquals(3, validationReportEntries.size());
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

      ValidationContextWithNetexEntitiesIndex validationContext = mock(
        ValidationContextWithNetexEntitiesIndex.class
      );

      CommonDataRepository commonDataRepository = mock(
        CommonDataRepository.class
      );
      when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

      when(validationContext.getAntuNetexData())
        .thenReturn(
          new AntuNetexData(
            validationReportId,
            netexEntitiesIndex,
            commonDataRepository,
            mock(StopPlaceRepository.class)
          )
        );

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
