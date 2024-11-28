package no.entur.antu.validation.validator.servicelink.distance;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.junit.jupiter.api.Test;

class UnexpectedDistanceInServiceLinkValidatorIntegrationTest {

  public static final String TEST_CODESPACE = "AVI";
  public static final String TEST_FILE_WITH_NO_SERVICE_LINKS =
    "_avinor_common_elements.xml";
  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testValidIncreasingPassingTime() throws IOException {
    ValidationReport validationReport = getValidationReport(
      TEST_FILE_WITH_NO_SERVICE_LINKS
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

      JAXBValidationContext validationContext = new JAXBValidationContext(
        validationReportId,
        netexEntitiesIndex,
        null,
        null,
        TEST_CODESPACE,
        testFile,
        Map.of()
      );

      UnexpectedDistanceInServiceLinkValidator unexpectedDistanceInServiceLinkValidator =
        new UnexpectedDistanceInServiceLinkValidator();

      unexpectedDistanceInServiceLinkValidator.validate(validationContext);
    }

    return testValidationReport;
  }
}
