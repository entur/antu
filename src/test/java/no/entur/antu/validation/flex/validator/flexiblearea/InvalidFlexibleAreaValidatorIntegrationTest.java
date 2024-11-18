package no.entur.antu.validation.flex.validator.flexiblearea;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.junit.jupiter.api.Test;

class InvalidFlexibleAreaValidatorIntegrationTest {

  private static final String _ATB_FLEXIBLE_SHARED_DATA =
    "_ATB_flexible_shared_data.xml";

  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testSelfIntersectingRingShouldBeReported() throws IOException {
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

      JAXBValidationContext validationContext = new JAXBValidationContext(
        validationReportId,
        netexEntitiesIndex,
        null,
        null,
        codeSpace,
        testFile,
        Map.of()
      );

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
