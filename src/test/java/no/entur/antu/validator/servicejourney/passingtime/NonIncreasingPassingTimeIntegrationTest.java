package no.entur.antu.validator.servicejourney.passingtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;

class NonIncreasingPassingTimeIntegrationTest {

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

      NonIncreasingPassingTime nonIncreasingPassingTime =
        new NonIncreasingPassingTime((code, message, dataLocation) ->
          new ValidationReportEntry(
            message,
            code,
            ValidationReportEntrySeverity.ERROR
          )
        );

      nonIncreasingPassingTime.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
