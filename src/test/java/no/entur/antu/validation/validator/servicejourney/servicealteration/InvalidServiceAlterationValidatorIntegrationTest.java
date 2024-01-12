package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.jaxb.CommonDataRepository;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.junit.jupiter.api.Test;

class InvalidServiceAlterationValidatorIntegrationTest {

  private static final String TEST_CODESPACE = "NSB";
  private static final String TEST_LINE_XML_FILE = "line.xml";
  private static final String TEST_FILE = "servicealteration/NSB_L1.xml";
  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testMissingServiceAlterationOnReplacedDSJs() throws IOException {
    List<ValidationIssue> issues = getValidationIssues(
      TEST_FILE,
      "${SERVICE_ALTERATION}",
      ""
    );
    assertEquals(1, issues.size());
  }

  @Test
  void testMismatchedServiceAlterationOnReplacedDSJs() throws IOException {
    List<ValidationIssue> issues = getValidationIssues(
      TEST_FILE,
      "${SERVICE_ALTERATION}",
      "<ServiceAlteration>cancellation</ServiceAlteration>"
    );
    assertEquals(1, issues.size());
  }

  @Test
  void testCorrectServiceAlterationOnReplacedDSJs() throws IOException {
    List<ValidationIssue> issues = getValidationIssues(
      TEST_FILE,
      "${SERVICE_ALTERATION}",
      "<ServiceAlteration>replaced</ServiceAlteration>"
    );
    assertTrue(issues.isEmpty());
  }

  private List<ValidationIssue> getValidationIssues(
    String testFile,
    String placeholder,
    String replacement
  ) throws IOException {
    String validationReportId = "Test1122";

    try (
      InputStream testDatasetAsStream = getClass()
        .getResourceAsStream('/' + testFile)
    ) {
      assert testDatasetAsStream != null;

      String netex = new String(
        testDatasetAsStream.readAllBytes(),
        StandardCharsets.UTF_8
      )
        .replace(placeholder, replacement);

      NetexEntitiesIndex netexEntitiesIndex = NETEX_PARSER.parse(
        IOUtils.toInputStream(netex)
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
