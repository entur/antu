package no.entur.antu.validation.validator.xpath.rules;

import java.util.List;
import java.util.Set;
import net.sf.saxon.s9api.XdmNode;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidateNoStopPlacesInImportedFlexDatasetTest {

  public static final String TEST_CODESPACE = "ATB";
  private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser(
    Set.of()
  );

  private final ValidateNotExist validateStopPlacesNotExist =
    new ValidateNotExist(
      "SiteFrame/stopPlaces",
      "stopPlaces not allowed in flexible shared files",
      "SITE_FRAME_IN_COMMON_FILE_1"
    );

  private static final String NETEX_SITE_FRAME_WITH_FLEXIBLE_STOP_PLACES =
    """
            <SiteFrame xmlns="http://www.netex.org.uk/netex" version="1" id="ATB:SiteFrame:1">
                <flexibleStopPlaces>
                    <FlexibleStopPlace version="0" id="ATB:FlexibleStopPlace:81182be4-d18a-50ff-8cba-903bd5672a59">
                    </FlexibleStopPlace>
                </flexibleStopPlaces>
            </SiteFrame>
            """;

  private static final String NETEX_SITE_FRAME_WITH_STOP_PLACES =
    """
            <SiteFrame xmlns="http://www.netex.org.uk/netex" version="1" id="ATB:SiteFrame:1">
                <stopPlaces>
                    <StopPlace version="0" id="NSR:StopPlace:81182">
                    </StopPlace>
                </stopPlaces>
            </SiteFrame>
            """;

  @Test
  void testStopPlacesNotExistsInSiteFrameShouldBeOk() {
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(
      NETEX_SITE_FRAME_WITH_FLEXIBLE_STOP_PLACES
    );
    XPathRuleValidationContext xpathValidationContext =
      new XPathRuleValidationContext(
        document,
        NETEX_XML_PARSER,
        TEST_CODESPACE,
        null
      );
    List<XPathValidationReportEntry> xPathValidationReportEntries =
      validateStopPlacesNotExist.validate(xpathValidationContext);
    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testStopPlacesExistsInSiteFrameShouldFail() {
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(
      NETEX_SITE_FRAME_WITH_STOP_PLACES
    );
    XPathRuleValidationContext xpathValidationContext =
      new XPathRuleValidationContext(
        document,
        NETEX_XML_PARSER,
        TEST_CODESPACE,
        null
      );
    List<XPathValidationReportEntry> xPathValidationReportEntries =
      validateStopPlacesNotExist.validate(xpathValidationContext);
    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }
}
