package no.entur.antu.validation.validator.servicejourney.passingtime;

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

class NonIncreasingTimetabledPassingTimeValidatorIntegrationTest {

  private static final String TEST_CODESPACE = "FLB";
  private static final String TEST_FILE_VALID =
    "FLB_FLB-Line-42_42_Flamsbana.xml";
  private static final String TEST_FILE_VALID_INVALID =
    "FLB_FLB-Line-42_42_Flamsbana_non_increasing_passing_time.xml";
  private static final String TEST_LINE_XML_FILE = "line.xml";

  private static final NetexParser NETEX_PARSER = new NetexParser();

  @Test
  void testValidIncreasingPassingTime() throws IOException {
    List<ValidationIssue> validationIssues = getValidationIssues(
      TEST_FILE_VALID
    );
    assertTrue(validationIssues.isEmpty());
  }

  @Test
  void testInValidIncreasingPassingTime() throws IOException {
    List<ValidationIssue> validationIssues = getValidationIssues(
      TEST_FILE_VALID_INVALID
    );
    assertEquals(3, validationIssues.size());
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

      NonIncreasingPassingTimeValidator nonIncreasingPassingTimeValidator =
        new NonIncreasingPassingTimeValidator();

      return nonIncreasingPassingTimeValidator.validate(validationContext);
    }
  }
}
