package no.entur.antu.validation.flex.validator.flexiblearea;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    ValidationReport testValidationReport = new ValidationReport(
      codeSpace,
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
      when(validationContext.getAntuNetexData()).thenReturn(antuNetexData);

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
