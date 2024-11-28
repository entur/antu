package no.entur.antu.validation.validator.xpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.validation.validator.xpath.rules.ValidateAllowedCodespaces;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidateAllowedCodespacesIntegrationTest {

  public static final String TEST_CODESPACE = "FLB";
  public static final String TEST_FILE_VALID_CODESPACE =
    "FLB_FLB-Line-42_42_Flamsbana.xml";
  public static final String TEST_FILE_VALID_INVALID_CODESPACE =
    "FLB_FLB-Line-42_42_Flamsbana_invalid_codespace.xml";

  private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser(
    Set.of("SiteFrame")
  );

  @Test
  void testValidCodeSpace() throws IOException {
    ValidateAllowedCodespaces validateAllowedCodespaces =
      new ValidateAllowedCodespaces();
    List<ValidationIssue> validationReportEntries = getValidationReportEntries(
      TEST_FILE_VALID_CODESPACE,
      TEST_CODESPACE,
      validateAllowedCodespaces
    );
    Assertions.assertTrue(validationReportEntries.isEmpty());
  }

  @Test
  void testInValidCodeSpace() throws IOException {
    ValidateAllowedCodespaces validateAllowedCodespaces =
      new ValidateAllowedCodespaces();
    List<ValidationIssue> validationReportEntries = getValidationReportEntries(
      TEST_FILE_VALID_INVALID_CODESPACE,
      TEST_CODESPACE,
      validateAllowedCodespaces
    );
    Assertions.assertFalse(validationReportEntries.isEmpty());
  }

  private List<ValidationIssue> getValidationReportEntries(
    String testFileValidCodespace,
    String testCodespace,
    ValidateAllowedCodespaces validateAllowedCodespaces
  ) throws IOException {
    InputStream testDatasetAsStream = getClass()
      .getResourceAsStream('/' + testFileValidCodespace);
    assert testDatasetAsStream != null;
    XdmNode document = NETEX_XML_PARSER.parseByteArrayToXdmNode(
      testDatasetAsStream.readAllBytes()
    );
    XPathRuleValidationContext validationContext =
      new XPathRuleValidationContext(
        document,
        NETEX_XML_PARSER,
        testCodespace,
        testFileValidCodespace
      );
    return validateAllowedCodespaces.validate(validationContext);
  }
}
