package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.junit.jupiter.api.Test;

class InvalidServiceAlterationValidatorIntegrationTest {

  public static final String TEST_CODESPACE = "NSB";
  public static final String TEST_FILE_INVALID = "servicealteration/NSB_L1.xml";
  public static final String TEST_FILE_VALID = "servicealteration/VYG_41.xml";
  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testMissingServiceAlterationOnReplacedDSJs() throws IOException {
    ValidationReport validationReport = getValidationReport(TEST_FILE_INVALID);
    assertEquals(2, validationReport.getValidationReportEntries().size());
  }

  @Test
  void testCorrectServiceAlterationOnReplacedDSJs() throws IOException {
    ValidationReport validationReport = getValidationReport(TEST_FILE_VALID);
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

      JAXBValidationContext validationContext = mock(
        JAXBValidationContext.class
      );

      NetexDataRepository commonDataRepository = mock(
        NetexDataRepository.class
      );
      when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

      when(validationContext.getValidationReportId())
        .thenReturn(validationReportId);
      when(validationContext.getNetexEntitiesIndex())
        .thenReturn(netexEntitiesIndex);
      when(validationContext.getNetexDataRepository())
        .thenReturn(commonDataRepository);
      when(validationContext.getStopPlaceRepository())
        .thenReturn(mock(StopPlaceRepository.class));

      InvalidServiceAlterationValidator invalidServiceAlterationValidator =
        new InvalidServiceAlterationValidator((code, message, dataLocation) ->
          new ValidationReportEntry(
            message,
            code,
            ValidationReportEntrySeverity.ERROR
          )
        );

      invalidServiceAlterationValidator.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
