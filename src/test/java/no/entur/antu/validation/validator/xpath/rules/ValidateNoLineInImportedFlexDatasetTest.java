package no.entur.antu.validation.validator.xpath.rules;

import java.util.List;
import java.util.Set;
import net.sf.saxon.s9api.XdmNode;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidateNoLineInImportedFlexDatasetTest {

  public static final String TEST_CODESPACE = "ATB";
  private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser(
    Set.of("SiteFrame")
  );

  private final ValidateNotExist validateLineNotExist = new ValidateNotExist(
    "ServiceFrame/lines/Line",
    "Line not allowed in imported flexible line files",
    "LINE_10"
  );

  private static final String NETEX_FRAGMENT_LINE =
    """
            <ServiceFrame xmlns="http://www.netex.org.uk/netex" id="ATB:ServiceFrame:1" version="2223">
              <lines>
                <Line id="ATB:Line:2_1" version="2223">
                </Line>
              </lines>
            </ServiceFrame>
            """;

  private static final String NETEX_FRAGMENT_FLEXIBLE_LINE =
    """
            <ServiceFrame xmlns="http://www.netex.org.uk/netex" id="ATB:ServiceFrame:1" version="2223">
              <lines>
                <FlexibleLine version="12" id="ATB:FlexibleLine:0e5b97ec-b755-5bd8-b9f1-f9a8db304139">
                </FlexibleLine>
              </lines>
            </ServiceFrame>
            """;

  @Test
  void testLineNotAllowedInServiceFrame() {
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(
      NETEX_FRAGMENT_LINE
    );
    XPathValidationContext xpathValidationContext = new XPathValidationContext(
      document,
      NETEX_XML_PARSER,
      TEST_CODESPACE,
      null
    );
    List<XPathValidationReportEntry> xPathValidationReportEntries =
      validateLineNotExist.validate(xpathValidationContext);
    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testFlexibleLineAllowedInServiceFrame() {
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(
      NETEX_FRAGMENT_FLEXIBLE_LINE
    );
    XPathValidationContext xpathValidationContext = new XPathValidationContext(
      document,
      NETEX_XML_PARSER,
      TEST_CODESPACE,
      null
    );
    List<XPathValidationReportEntry> xPathValidationReportEntries =
      validateLineNotExist.validate(xpathValidationContext);
    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }
}
