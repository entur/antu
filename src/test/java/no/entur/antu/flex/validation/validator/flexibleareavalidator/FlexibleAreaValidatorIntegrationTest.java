package no.entur.antu.flex.validation.validator.flexibleareavalidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;

class FlexibleAreaValidatorIntegrationTest {

  public static final String _ATB_FLEXIBLE_SHARED_DATA =
    "_ATB_flexible_shared_data_2.xml";

  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testFlexibleAreaValidator() throws IOException {
    ValidationReport validationReport = getValidationReport(
      _ATB_FLEXIBLE_SHARED_DATA,
      "ATB"
    );
    assertEquals(7, validationReport
      .getValidationReportEntries()
      .size());
  }

  private ValidationReport getValidationReport(String testFile, String codeSpace)
    throws IOException {
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
      when(validationContext.getNetexEntitiesIndex())
        .thenReturn(netexEntitiesIndex);

      when(validationContext.isCommonFile()).thenReturn(true);

      FlexibleAreaValidator flexibleAreaValidator =
        new FlexibleAreaValidator(
          (code, message, dataLocation) ->
            new ValidationReportEntry(
              message,
              code,
              ValidationReportEntrySeverity.ERROR
            )
        );

      flexibleAreaValidator.validate(testValidationReport, validationContext);
    }

    return testValidationReport;
  }
}