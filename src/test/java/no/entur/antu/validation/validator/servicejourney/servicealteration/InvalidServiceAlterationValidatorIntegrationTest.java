package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.jaxb.CommonDataRepository;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.junit.jupiter.api.Test;

class InvalidServiceAlterationValidatorIntegrationTest {

  private static final String TEST_CODESPACE = "NSB";
  private static final String TEST_LINE_XML_FILE = "line.xml";
  private static final String TEST_FILE_INVALID =
    "servicealteration/NSB_L1.xml";
  private static final String TEST_FILE_VALID = "servicealteration/VYG_41.xml";
  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testMissingServiceAlterationOnReplacedDSJs() throws IOException {
    List<ValidationIssue> issues = getValidationIssues(TEST_FILE_INVALID);
    assertEquals(2, issues.size());
  }

  @Test
  void testCorrectServiceAlterationOnReplacedDSJs() throws IOException {
    List<ValidationIssue> issues = getValidationIssues(TEST_FILE_VALID);
    assertTrue(issues.isEmpty());
  }

  private List<ValidationIssue> getValidationIssues(String testFile)
    throws IOException {
    String validationReportId = "Test1122";

    try (
      InputStream testDatasetAsStream = getClass()
        .getResourceAsStream('/' + testFile)
    ) {
      assert testDatasetAsStream != null;
      NetexEntitiesIndex netexEntitiesIndex = NETEX_PARSER.parse(
        testDatasetAsStream
      );

      CommonDataRepository commonDataRepository = mock(
        CommonDataRepository.class
      );
      when(commonDataRepository.hasSharedScheduledStopPoints(anyString()))
        .thenReturn(true);

      JAXBValidationContext validationContext = new JAXBValidationContext(
        validationReportId,
        netexEntitiesIndex,
        commonDataRepository,
        null,
        TEST_CODESPACE,
        TEST_LINE_XML_FILE,
        Map.of()
      );

      InvalidServiceAlterationValidator invalidServiceAlterationValidator =
        new InvalidServiceAlterationValidator();

      return invalidServiceAlterationValidator.validate(validationContext);
    }
  }
}
