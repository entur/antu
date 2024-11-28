package no.entur.antu.validation.validator.xpath.rules;

import java.util.List;
import java.util.Set;
import net.sf.saxon.s9api.XdmNode;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidateNSRCodespaceTest {

  public static final String TEST_CODESPACE = "FLB";
  private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser(
    Set.of("SiteFrame")
  );

  private static final String NETEX_FRAGMENT =
    """
                                    <codespaces xmlns="http://www.netex.org.uk/netex">
                                      <Codespace id="nsr">
                                        <Xmlns>NSR</Xmlns>
                                        <XmlnsUrl>${XML_NAMESPACE_URL}</XmlnsUrl>
                                      </Codespace>
                                    </codespaces>
            """;

  @Test
  void testMissingNSRCodeSpace() {
    ValidateNSRCodespace validateNSRCodespace = new ValidateNSRCodespace();
    String fragmentWithInvalidCodespace = NETEX_FRAGMENT.replace(
      "${XML_NAMESPACE_URL}",
      "http://www.rutebanken.org/ns/nsr-invalid"
    );
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(
      fragmentWithInvalidCodespace
    );
    XPathRuleValidationContext xpathValidationContext =
      new XPathRuleValidationContext(
        document,
        NETEX_XML_PARSER,
        TEST_CODESPACE,
        null
      );
    List<ValidationIssue> xPathValidationReportEntries =
      validateNSRCodespace.validate(xpathValidationContext);
    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testValidNSRCodeSpace() {
    ValidateNSRCodespace validateNSRCodespace = new ValidateNSRCodespace();
    String fragmentWithValidCodespace = NETEX_FRAGMENT.replace(
      "${XML_NAMESPACE_URL}",
      "http://www.rutebanken.org/ns/nsr"
    );
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(
      fragmentWithValidCodespace
    );
    XPathRuleValidationContext xpathValidationContext =
      new XPathRuleValidationContext(
        document,
        NETEX_XML_PARSER,
        TEST_CODESPACE,
        null
      );
    List<ValidationIssue> xPathValidationReportEntries =
      validateNSRCodespace.validate(xpathValidationContext);
    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }
}
